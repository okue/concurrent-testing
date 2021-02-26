import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.loop
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.lincheck.LinChecker
import org.jetbrains.kotlinx.lincheck.annotations.Operation
import org.jetbrains.kotlinx.lincheck.annotations.Param
import org.jetbrains.kotlinx.lincheck.paramgen.IntGen
import org.jetbrains.kotlinx.lincheck.strategy.managed.modelchecking.ModelCheckingCTest
import org.jetbrains.kotlinx.lincheck.verifier.VerifierState
import org.junit.jupiter.api.Test

@ModelCheckingCTest(minimizeFailedScenario = true)
@Param(name = "v", gen = IntGen::class, conf = "-2:2")
class StackTest : VerifierState() {

    private val s = Stack<Int>()

    @Operation
    fun push(@Param(name = "v") value: Int) = s.push(value)

    @Operation
    fun pop() = s.pop()

    @Operation
    fun size() = s.size

    @Test
    fun runTest() = LinChecker.check(this::class.java)

    @Test
    fun testEquals() {
        val s1 = Stack<Int>()
        val s2 = Stack<Int>()
        (1..10).forEach { s1.push(it); s2.push(it) }

        assertThat(s1).isEqualTo(s2)
        assertThat(s1.hashCode()).isEqualTo(s2.hashCode())
        assertThat(s1.size).isEqualTo(10)

        s1.top.value.asSequence().forEach {
            print("${it.value} -> ")
        }
        println()
        println(s1.hashCode())

        s1.push(0)
        assertThat(s1).isNotEqualTo(s2)
        assertThat(s1.hashCode()).isNotEqualTo(s2.hashCode())
        println(s1.hashCode())

        s1.pop()
        assertThat(s1.size).isEqualTo(10)
        assertThat(s1).isEqualTo(s2)
        assertThat(s1.hashCode()).isEqualTo(s2.hashCode())
        println(s1.hashCode())

        s1.push(100)
        s2.push(100)
        assertThat(s1).isEqualTo(s2)
        assertThat(s1.hashCode()).isEqualTo(s2.hashCode())
    }

    override fun extractState(): Any {
        return s
    }
}

class Stack<T> {
    internal val top = atomic<Node<T>?>(null)
    private val _size = atomic(0)

    fun push(value: T): Unit = top.loop { cur ->
        val newTop = Node(cur, value)
        if (top.compareAndSet(cur, newTop)) { // try to add
            _size.incrementAndGet() // <-- INCREMENT SIZE
            return
        }
    }

    fun pop(): T? = top.loop { cur ->
        if (cur == null) return null // is stack empty?
        if (top.compareAndSet(cur, cur.next)) { // try to retrieve
            _size.decrementAndGet() // <-- DECREMENT SIZE
            return cur.value
        }
    }

    val size: Int get() = _size.value

    override fun equals(other: Any?): Boolean {
        if (other !is Stack<*>) return false
        if (this === other) return true

        return this.top.value == other.top.value
    }

    override fun hashCode(): Int {
        return top.value.hashCode()
    }
}

data class Node<T>(val next: Node<T>?, val value: T)

internal fun <T> Node<T>?.asSequence(): Sequence<Node<T>> {
    return generateSequence(this) { it.next }
}
