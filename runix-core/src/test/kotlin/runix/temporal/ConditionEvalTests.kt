package runix.temporal

import runix.temporal.time.Time
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * Unit tests for ConditionEval.
 * Covers invert(), mergeAll(), and mergeAny() behaviors and edge cases.
 */
class ConditionEvalTests {

    // ------------------------
    // invert() tests
    // ------------------------

    @Test
    fun `invert True returns False`() {
        assertSame(ConditionEval.False, ConditionEval.True.invert())
    }

    @Test
    fun `invert False returns True`() {
        assertSame(ConditionEval.True, ConditionEval.False.invert())
    }

    @Test
    fun `invert Delayed returns same instance`() {
        val mark = Time.markNow()
        val delayed = ConditionEval.Delayed(mark)
        assertSame(delayed, delayed.invert())
    }

    // ------------------------
    // mergeAll() tests (AND semantics)
    // ------------------------

    @Test
    fun `mergeAll empty list returns True`() {
        assertSame(ConditionEval.True, ConditionEval.mergeAll(emptyList()))
    }

    @Test
    fun `mergeAll single True returns True`() {
        assertSame(ConditionEval.True, ConditionEval.mergeAll(listOf(ConditionEval.True)))
    }

    @Test
    fun `mergeAll single False returns False`() {
        assertSame(ConditionEval.False, ConditionEval.mergeAll(listOf(ConditionEval.False)))
    }

    @Test
    fun `mergeAll True and False returns False`() {
        val result = ConditionEval.mergeAll(listOf(ConditionEval.True, ConditionEval.False))
        assertSame(ConditionEval.False, result)
    }

    @Test
    fun `mergeAll single Delayed returns that Delayed`() {
        val mark = Time.markNow()
        val delayed = ConditionEval.Delayed(mark)
        val result = ConditionEval.mergeAll(listOf(delayed))
        assertEquals(delayed, result)
    }

    @Test
    fun `mergeAll multiple Delayed returns earliest mark`() {
        val m1 = Time.markNow()
        // ensure a different timestamp
        Thread.sleep(1)
        val m2 = Time.markNow()

        val d1 = ConditionEval.Delayed(m1)
        val d2 = ConditionEval.Delayed(m2)
        val result = ConditionEval.mergeAll(listOf(d2, d1))
        // earliest of m1 and m2 is m1
        require(result is ConditionEval.Delayed)
        assertEquals(m1, result.nextCheckAt)
    }

    // ------------------------
    // mergeAny() tests (OR semantics)
    // ------------------------

    @Test
    fun `mergeAny empty list returns False`() {
        assertSame(ConditionEval.False, ConditionEval.mergeAny(emptyList()))
    }

    @Test
    fun `mergeAny single True returns True`() {
        assertSame(ConditionEval.True, ConditionEval.mergeAny(listOf(ConditionEval.True)))
    }

    @Test
    fun `mergeAny single False returns False`() {
        assertSame(ConditionEval.False, ConditionEval.mergeAny(listOf(ConditionEval.False)))
    }

    @Test
    fun `mergeAny True and False returns True`() {
        val result = ConditionEval.mergeAny(listOf(ConditionEval.False, ConditionEval.True))
        assertSame(ConditionEval.True, result)
    }

    @Test
    fun `mergeAny single Delayed returns that Delayed`() {
        val mark = Time.markNow()
        val delayed = ConditionEval.Delayed(mark)
        val result = ConditionEval.mergeAny(listOf(delayed))
        assertEquals(delayed, result)
    }

    @Test
    fun `mergeAny multiple Delayed returns earliest mark`() {
        val m1 = Time.markNow()
        Thread.sleep(1)
        val m2 = Time.markNow()

        val d1 = ConditionEval.Delayed(m1)
        val d2 = ConditionEval.Delayed(m2)
        val result = ConditionEval.mergeAny(listOf(d2, d1))
        require(result is ConditionEval.Delayed)
        assertEquals(m1, result.nextCheckAt)
    }
}