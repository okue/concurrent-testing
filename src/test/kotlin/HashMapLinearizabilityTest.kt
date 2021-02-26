import org.jetbrains.kotlinx.lincheck.LinChecker
import org.jetbrains.kotlinx.lincheck.annotations.Operation
import org.jetbrains.kotlinx.lincheck.annotations.Param
import org.jetbrains.kotlinx.lincheck.paramgen.IntGen
import org.jetbrains.kotlinx.lincheck.strategy.managed.modelchecking.ModelCheckingCTest
import org.jetbrains.kotlinx.lincheck.verifier.VerifierState
import org.junit.jupiter.api.Test

@ModelCheckingCTest(minimizeFailedScenario = false)
// @StressCTest(minimizeFailedScenario = false)
@Param(name = "key", gen = IntGen::class, conf = "-3:3")
class HashMapLinearizabilityTest : VerifierState() {
    private val map = HashMap<Int, Int>();

    @Operation
    fun put(@Param(name = "key") key: Int, value: Int): Int? {
        return map.put(key, value)
    }

    @Operation
    fun get(@Param(name = "key") key: Int): Int? {
        return map[key]
    }

    @Test
    fun test() = LinChecker.check(this::class.java)

    override fun extractState(): Any = map
}
