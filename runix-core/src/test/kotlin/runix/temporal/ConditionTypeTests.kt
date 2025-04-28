package runix.temporal

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Unit tests for ConditionType.
 * Verifies retentionWindow properties and that compileExpression(key) delegates
 * to the correct BooleanTracker methods via TemporalEngine.
 */
class ConditionTypeTests {

    @BeforeTest
    fun setup() {
        // Mock the TemporalEngine singleton
        mockkObject(TemporalEngine)
    }

    @AfterTest
    fun teardown() {
        // Clear the TemporalEngine mock
        unmockkObject(TemporalEngine)
    }

    @Test
    fun `Persisted retentionWindow is zero`() {
        val cond = ConditionType.Persisted(5.seconds)
        assertEquals(Duration.ZERO, cond.retentionWindow)
    }

    @Test
    fun `Persisted compileExpression calls hasPersistedFor and returns its result`() {
        val duration = 3.seconds
        val key = "persistKey"
        val cond = ConditionType.Persisted(duration)

        // Stub tracker
        val fakeTracker = mockk<BooleanTracker> {
            every { hasPersistedFor(duration) } returns ConditionEval.True
        }
        every { TemporalEngine.getBooleanTracker(key) } returns fakeTracker

        // Compile and invoke
        val expr = cond.compileExpression(key)
        assertEquals(ConditionEval.True, expr())
    }

    @Test
    fun `WasStable retentionWindow is zero`() {
        val cond = ConditionType.WasStable(7.seconds)
        assertEquals(Duration.ZERO, cond.retentionWindow)
    }

    @Test
    fun `WasStable compileExpression calls hasBeenStableFor and returns its result`() {
        val duration = 2.seconds
        val key = "stableKey"
        val cond = ConditionType.WasStable(duration)

        val fakeTracker = mockk<BooleanTracker> {
            every { hasBeenStableFor(duration) } returns ConditionEval.False
        }
        every { TemporalEngine.getBooleanTracker(key) } returns fakeTracker

        val expr = cond.compileExpression(key)
        assertEquals(ConditionEval.False, expr())
    }

    @Test
    fun `IsTrueNow retentionWindow is zero`() {
        val cond = ConditionType.IsTrueNow
        assertEquals(Duration.ZERO, cond.retentionWindow)
    }

    @Test
    fun `IsTrueNow compileExpression calls isTrue and returns its result`() {
        val key = "instantKey"
        val cond = ConditionType.IsTrueNow

        val fakeTracker = mockk<BooleanTracker> {
            every { isTrue() } returns ConditionEval.True
        }
        every { TemporalEngine.getBooleanTracker(key) } returns fakeTracker

        val expr = cond.compileExpression(key)
        assertEquals(ConditionEval.True, expr())
    }

    @Test
    fun `WasEverTrue retentionWindow matches within`() {
        val within = 4.seconds
        val cond = ConditionType.WasEverTrue(within)
        assertEquals(within, cond.retentionWindow)
    }

    @Test
    fun `WasEverTrue compileExpression calls wasEverTrue and returns its result`() {
        val within = 1.seconds
        val key = "everTrueKey"
        val cond = ConditionType.WasEverTrue(within)

        val fakeTracker = mockk<BooleanTracker> {
            every { wasEverTrue() } returns ConditionEval.True
        }
        every { TemporalEngine.getBooleanTracker(key) } returns fakeTracker

        val expr = cond.compileExpression(key)
        assertEquals(ConditionEval.True, expr())
    }

    @Test
    fun `HasFluctuated retentionWindow matches within`() {
        val within = 6.seconds
        val cond = ConditionType.HasFluctuated(within)
        assertEquals(within, cond.retentionWindow)
    }

    @Test
    fun `HasFluctuated compileExpression calls hasFluctuated and returns its result`() {
        val within = 2.seconds
        val key = "fluctKey"
        val cond = ConditionType.HasFluctuated(within)

        val fakeTracker = mockk<BooleanTracker> {
            every { hasFluctuated() } returns ConditionEval.False
        }
        every { TemporalEngine.getBooleanTracker(key) } returns fakeTracker

        val expr = cond.compileExpression(key)
        assertEquals(ConditionEval.False, expr())
    }

    @Test
    fun `PersistedForAtLeast retentionWindow matches within`() {
        val target = 3.seconds
        val within = 8.seconds
        val cond = ConditionType.PersistedForAtLeast(target, within)
        assertEquals(within, cond.retentionWindow)
    }

    @Test
    fun `PersistedForAtLeast compileExpression calls hasPersistedForAtLeast and returns its result`() {
        val target = 5.seconds
        val within = 10.seconds
        val key = "atLeastKey"
        val cond = ConditionType.PersistedForAtLeast(target, within)

        val fakeTracker = mockk<BooleanTracker> {
            every { hasPersistedForAtLeast(target) } returns ConditionEval.True
        }
        every { TemporalEngine.getBooleanTracker(key) } returns fakeTracker

        val expr = cond.compileExpression(key)
        assertEquals(ConditionEval.True, expr())
    }
}