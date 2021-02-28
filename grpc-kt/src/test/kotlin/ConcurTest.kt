import example.hello.Hello.HelloReply
import example.hello.Hello.HelloRequest
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.ServerCall
import io.grpc.Status
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.jetbrains.kotlinx.lincheck.LinChecker
import org.jetbrains.kotlinx.lincheck.annotations.OpGroupConfig
import org.jetbrains.kotlinx.lincheck.annotations.Operation
import org.jetbrains.kotlinx.lincheck.strategy.stress.StressCTest
import org.jetbrains.kotlinx.lincheck.verifier.VerifierState
import org.junit.jupiter.api.Test
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@InternalCoroutinesApi
@OpGroupConfig(name = "request", nonParallel = true)
@StressCTest(
    minimizeFailedScenario = false,
    iterations = 10,
)
class ServerCallsTest : VerifierState() {
    @Volatile
    private var closed = false

    private val call: MockServerCall<HelloRequest, HelloReply> = MockServerCall(HelloRequest.getDefaultInstance())

    init {
        call.setListener(
            serverCallListener(EmptyCoroutineContext, call) {
                it.map { HelloReply.getDefaultInstance() }
            }
        )
    }

    @Test
    fun test() = LinChecker.check(this::class.java)

    @Operation(group = "request")
    fun request() {
        if (closed) return
        call.request(1)
    }

    @Operation(runOnce = true, handleExceptionsAsResult = [ClosedSendChannelException::class])
    fun close() {
        call.close(Status.OK, Metadata())
        closed = true
    }

    override fun extractState(): Any = call.state
}

@InternalCoroutinesApi
private fun <RequestT, ResponseT> serverCallListener(
    context: CoroutineContext,
    call: ServerCall<RequestT, ResponseT>,
    implementation: (Flow<RequestT>) -> Flow<ResponseT>
): ServerCall.Listener<RequestT> {
    val requestsChannel = Channel<RequestT>(1)
    val requests = flow {
        try {
            for (request in requestsChannel) {
                emit(request)
                call.request(1)
            }
        } catch (e: Exception) {
            requestsChannel.cancel(
                CancellationException("Exception thrown while collecting requests", e)
            )
            call.request(1) // make sure we don't cause backpressure
            throw e
        }
    }

    // https://github.com/grpc/grpc-kotlin/pull/157/files
    val rpcScope = CoroutineScope(context)
    val rpcJob = rpcScope.async {
        runCatching {
            implementation(requests).collect {
                call.sendMessage(it)
            }
        }.exceptionOrNull()
    }
    rpcJob.invokeOnCompletion { cause ->
        val closeStatus = when (cause) {
            null -> Status.OK
            is CancellationException -> Status.CANCELLED.withCause(cause)
            else -> Status.fromThrowable(cause)
        }
        val trailers = cause?.let { Status.trailersFromThrowable(it) } ?: Metadata()
        call.close(closeStatus, trailers)
    }

    return object : ServerCall.Listener<RequestT>() {
        var isReceiving = true

        override fun onCancel() {
            rpcScope.cancel("Cancellation received from client")
        }

        override fun onMessage(message: RequestT) {
            if (isReceiving) {
                try {
                    requestsChannel.offer(message)
                } catch (e: CancellationException) {
                    // we don't want any more client input; swallow it
                    isReceiving = false
                }
            }
            if (!isReceiving) {
                call.request(1) // do not exert backpressure
            }
        }

        override fun onHalfClose() {
            requestsChannel.close()
        }

        override fun onReady() {
            // nothing
        }
    }
}

private class MockServerCall<Req, Res>(
    private val message: Req,
) : ServerCall<Req, Res>() {

    private var closed: Boolean = false

    private var cancelled: Boolean = false

    private var messageSent: Int = 0

    private var listener: Listener<Req>? = null

    val state: Triple<Boolean, Boolean, Int>
        get() = Triple(closed, cancelled, messageSent)

    fun setListener(listener: Listener<Req>) {
        this.listener = listener
    }

    override fun request(numMessages: Int) {
        if (closed) return
        val listener = checkNotNull(listener)
        repeat(times = numMessages) {
            listener.onMessage(message)
        }
    }

    override fun sendHeaders(headers: Metadata) {
        // do nothing
    }

    override fun sendMessage(message: Res) {
        messageSent++
    }

    override fun close(status: Status, trailers: Metadata) {
        if (!closed) {
            listener?.onHalfClose()
            closed = true
        }
    }

    override fun isCancelled(): Boolean {
        return cancelled
    }

    override fun getMethodDescriptor(): MethodDescriptor<Req, Res>? {
        return null
    }

    override fun hashCode(): Int {
        return this.state.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is MockServerCall<*, *>) return false
        if (this === other) return true
        return this.state == other.state
    }
}
