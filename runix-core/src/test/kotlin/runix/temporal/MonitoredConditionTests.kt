package runix.temporal

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Unit tests for MonitoredCondition.
 * Covers flows(), compile(getKey), compile(monitorName), and evaluator logic for
 * Leaf, AllOf, AnyOf, and Not.
 */
class MonitoredConditionTests {

    private val flowA = MutableStateFlow(false)
    private val flowB = MutableStateFlow(true)

    @BeforeTest
    fun setup() {
        // No global mocks needed here
    }

    @AfterTest
    fun tearDown() {
        // Clean up if necessary
    }

    // ------------------------
    // flows() tests
    // ------------------------

    @Test
    fun `Leaf flows returns single flow`() {
        val leaf = MonitoredCondition.Leaf(flowA, mockk(relaxed = true))
        assertEquals(setOf(flowA), leaf.flows())
    }

    @Test
    fun `AllOf flows returns union of child flows`() {
        val leafA = MonitoredCondition.Leaf(flowA, mockk(relaxed = true))
        val leafB = MonitoredCondition.Leaf(flowB, mockk(relaxed = true))
        val leafC = MonitoredCondition.Leaf(flowB, mockk(relaxed = true))
        val all = MonitoredCondition.AllOf(listOf(leafA, leafB, leafC))
        assertEquals(setOf(flowA, flowB), all.flows())
    }

    @Test
    fun `AnyOf flows returns union of child flows`() {
        val leafA = MonitoredCondition.Leaf(flowA, mockk(relaxed = true))
        val leafB = MonitoredCondition.Leaf(flowB, mockk(relaxed = true))
        val leafC = MonitoredCondition.Leaf(flowB, mockk(relaxed = true))
        val any = MonitoredCondition.AnyOf(listOf(leafA, leafB, leafC))
        assertEquals(setOf(flowA, flowB), any.flows())
    }

    @Test
    fun `Not flows returns same as child flows`() {
        val leaf = MonitoredCondition.Leaf(flowA, mockk(relaxed = true))
        val not = MonitoredCondition.Not(leaf)
        assertEquals(setOf(flowA), not.flows())
    }

    // ------------------------
    // compile(getKey) tests (Leaf)
    // ------------------------

    @Test
    fun `Leaf compile returns evaluator and registration`() {
        // Stub ConditionType to return False and a retention of 2 seconds
        val fakeType = mockk<ConditionType> {
            every { compileExpression("key1") } returns { ConditionEval.False }
            every { retentionWindow } returns 2.seconds
        }
        val leaf = MonitoredCondition.Leaf(flowA, fakeType)
        val getKey: FlowKeyProvider = { "key1" }

        val (expr, regs) = leaf.compile(getKey)

        // Evaluator yields our stubbed value
        assertEquals(ConditionEval.False, expr())
        // Registration list correct
        assertEquals(1, regs.size)
        assertEquals("key1", regs[0].key)
        assertEquals(flowA,   regs[0].flow)
        assertEquals(2.seconds, regs[0].requiredRetention)
    }

    // ------------------------
    // compile(getKey) tests (AllOf, AnyOf, Not evaluator logic)
    // ------------------------

    @Test
    fun `AllOf compile combines evaluators with AND semantics`() {
        // Two leaves: one True, one False
        val typeTrue = mockk<ConditionType> {
            every { compileExpression(any()) } returns { ConditionEval.True }
            every { retentionWindow } returns Duration.ZERO
        }
        val typeFalse = mockk<ConditionType> {
            every { compileExpression(any()) } returns { ConditionEval.False }
            every { retentionWindow } returns Duration.ZERO
        }
        val leafA = MonitoredCondition.Leaf(flowA, typeTrue)
        val leafB = MonitoredCondition.Leaf(flowB, typeFalse)
        val all = MonitoredCondition.AllOf(listOf(leafA, leafB))
        val (expr, regs) = all.compile { flow -> if (flow == flowA) "A" else "B" }

        // AND(True, False) -> False
        assertEquals(ConditionEval.False, expr())
        // Both registrations are propagated
        assertEquals(2, regs.size)
    }

    @Test
    fun `AnyOf compile combines evaluators with OR semantics`() {
        // Two leaves: one False, one True
        val typeTrue = mockk<ConditionType> {
            every { compileExpression(any()) } returns { ConditionEval.True }
            every { retentionWindow } returns Duration.ZERO
        }
        val typeFalse = mockk<ConditionType> {
            every { compileExpression(any()) } returns { ConditionEval.False }
            every { retentionWindow } returns Duration.ZERO
        }
        val leafA = MonitoredCondition.Leaf(flowA, typeFalse)
        val leafB = MonitoredCondition.Leaf(flowB, typeTrue)
        val any = MonitoredCondition.AnyOf(listOf(leafA, leafB))
        val (expr, regs) = any.compile { flow -> if (flow == flowA) "A" else "B" }

        // OR(False, True) -> True
        assertEquals(ConditionEval.True, expr())
        assertEquals(2, regs.size)
    }

    @Test
    fun `Not compile inverts child evaluator result`() {
        val typeTrue = mockk<ConditionType> {
            every { compileExpression(any()) } returns { ConditionEval.True }
            every { retentionWindow } returns Duration.ZERO
        }
        val leaf = MonitoredCondition.Leaf(flowA, typeTrue)
        val not = MonitoredCondition.Not(leaf)
        val (expr, regs) = not.compile { _ -> "X" }

        // invert(True) -> False
        assertEquals(ConditionEval.False, expr())
        // Registration list is same as child
        assertEquals(1, regs.size)
    }

    // ------------------------
    // compile(monitorName) tests
    // ------------------------

    @Test
    fun `compile(monitorName) generates sequential keys`() {
        // Two leaves for key-naming ordering
        val type = mockk<ConditionType> {
            every { compileExpression(any()) } returns { ConditionEval.True }
            every { retentionWindow } returns Duration.ZERO
        }
        val leafA = MonitoredCondition.Leaf(flowA, type)
        val leafB = MonitoredCondition.Leaf(flowB, type)
        val all = MonitoredCondition.AllOf(listOf(leafA, leafB))

        val cm = all.compile("MyMonitor")
        val regs = cm.flowRegistrations

        assertEquals(2, regs.size)
        assertEquals("MyMonitor::0", regs[0].key)
        assertEquals(flowA, regs[0].flow)
        assertEquals("MyMonitor::1", regs[1].key)
        assertEquals(flowB, regs[1].flow)
    }
}