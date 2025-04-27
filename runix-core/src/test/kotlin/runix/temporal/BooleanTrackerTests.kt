import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import runix.internal.RuntimeScope
import runix.temporal.BooleanTracker
import runix.temporal.ConditionEval
import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class BooleanTrackerTests {

    private fun runTrackerTest(
        initial: Boolean,
        retention: Duration,
        block: suspend TestScope.(flow: MutableStateFlow<Boolean>, tracker: BooleanTracker) -> Unit
    ) {
        runTest {
            RuntimeScope.overrideScopeForTesting(this)
            val flow = MutableStateFlow(initial)
            val tracker = BooleanTracker(flow, retention) { }
            try {
                block(flow, tracker)
            } finally {
                tracker.dispose()
            }
        }
    }

    // ----------------------------------------------------------------------------------------------------
    // Initialization
    // ----------------------------------------------------------------------------------------------------

    @Test fun `(initialization) should initialize with flow false`() = runTrackerTest(initial = false, retention = 500.milliseconds) { _, tracker ->
        assertEquals(false, tracker.isTrue() == ConditionEval.True)
    }

    @Test fun `(initialization) should initialize with flow true`() = runTrackerTest(initial = true, retention = 500.milliseconds) { _, tracker ->
        assertEquals(true, tracker.isTrue() == ConditionEval.True)
    }

    @Test fun `(initialization) should start with one value in history`() = runTrackerTest(initial = true, retention = 500.milliseconds) { _, tracker ->
        assertEquals(1, tracker.valueHistory.size)
    }

    // ----------------------------------------------------------------------------------------------------
    // Updates and Appends
    // ----------------------------------------------------------------------------------------------------

    @Test fun `(updates) should append only when value changes`() = runTrackerTest(initial = false, retention = 500.milliseconds) { flow, tracker ->
        flow.emit(false)
        advanceUntilIdle()
        flow.emit(true)
        advanceUntilIdle()
        flow.emit(true)
        advanceUntilIdle()
        assertEquals(2, tracker.valueHistory.size)
    }

    @Test fun `(updates) should maintain correct last value`() = runTrackerTest(initial = true, retention = 500.milliseconds) { flow, tracker ->
        flow.emit(false)
        advanceUntilIdle()
        assertEquals(ConditionEval.False, tracker.isTrue())
    }

    // ----------------------------------------------------------------------------------------------------
    // Pruning
    // ----------------------------------------------------------------------------------------------------

    @Test fun `(pruning) should prune closed periods before window`() = runTrackerTest(initial = true, retention = 300.milliseconds) { flow, tracker ->
        flow.emit(false)
        advanceUntilIdle()
        Thread.sleep(400)
        advanceUntilIdle()
        flow.emit(true)
        advanceUntilIdle()
        assertTrue(tracker.valueHistory.size <= 2)
    }

    @Test fun `(pruning) should preserve open true periods during pruning`() = runTrackerTest(initial = true, retention = 300.milliseconds) { flow, tracker ->
        Thread.sleep(400)
        advanceUntilIdle()
        assertEquals(1, tracker.valueHistory.size)
    }

    @Test fun `(pruning) should not retain periods fully outside window`() = runTrackerTest(initial = true, retention = 300.milliseconds) { flow, tracker ->
        flow.emit(false)
        advanceUntilIdle()
        Thread.sleep(400)
        advanceUntilIdle()
        flow.emit(true)
        advanceUntilIdle()
        assertTrue(tracker.valueHistory.size <= 2)
    }

    @Test fun `(pruning) should behave correctly in memoryless mode`() = runTrackerTest(initial = true, retention = Duration.ZERO) { flow, tracker ->
        repeat(10) {
            flow.emit(!flow.value)
            advanceUntilIdle()
        }
        assertEquals(1, tracker.valueHistory.size)
    }

    // ----------------------------------------------------------------------------------------------------
    // Persistence (Memoryless)
    // ----------------------------------------------------------------------------------------------------

    @Test fun `(persistence) should return true when persisted duration met`() = runTrackerTest(initial = true, retention = Duration.ZERO) { _, tracker ->
        advanceUntilIdle()
        Thread.sleep(200)
        assertEquals(ConditionEval.True, tracker.hasPersistedFor(100.milliseconds))
    }

    @Test fun `(persistence) should delay when persisted duration not met`() = runTrackerTest(initial = true, retention = Duration.ZERO) { _, tracker ->
        assertTrue(tracker.hasPersistedFor(500.milliseconds) is ConditionEval.Delayed)
    }

    @Test fun `(persistence) should return false if signal is false`() = runTrackerTest(initial = false, retention = Duration.ZERO) { _, tracker ->
        assertEquals(ConditionEval.False, tracker.hasPersistedFor(100.milliseconds))
    }

    // ----------------------------------------------------------------------------------------------------
    // Persistence Accumulation (Memoryful)
    // ----------------------------------------------------------------------------------------------------

    @Test fun `(persistence) should accumulate multiple true periods within window`() = runTrackerTest(initial = false, retention = 1.seconds) { flow, tracker ->
        flow.emit(true)
        advanceUntilIdle()
        Thread.sleep(150)
        flow.emit(false)
        advanceUntilIdle()
        flow.emit(true)
        advanceUntilIdle()
        Thread.sleep(150)
        advanceUntilIdle()
        assertEquals(ConditionEval.True, tracker.hasPersistedForAtLeast(200.milliseconds))
    }

    @Test fun `(persistence) should delay if cumulative true time less than target`() = runTrackerTest(initial = false, retention = 1.seconds) { flow, tracker ->
        flow.emit(true)
        advanceUntilIdle()
        Thread.sleep(100)
        flow.emit(false)
        advanceUntilIdle()
        assertTrue(tracker.hasPersistedForAtLeast(200.milliseconds) is ConditionEval.Delayed)
    }

    // ----------------------------------------------------------------------------------------------------
    // Transient
    // ----------------------------------------------------------------------------------------------------

    @Test fun `(transient) should detect true transient inside window`() = runTrackerTest(initial = false, retention = 500.milliseconds) { flow, tracker ->
        flow.emit(true)
        advanceUntilIdle()
        Thread.sleep(50)
        assertEquals(ConditionEval.True, tracker.wasEverTrue())
    }

    @Test fun `(transient) should delay if no true transient seen`() = runTrackerTest(initial = false, retention = 500.milliseconds) { _, tracker ->
        assertTrue(tracker.wasEverTrue() is ConditionEval.Delayed)
    }

    // ----------------------------------------------------------------------------------------------------
    // Fluctuation
    // ----------------------------------------------------------------------------------------------------

    @Test fun `(fluctuation) should detect true to false fluctuation`() = runTrackerTest(initial = false, retention = 500.milliseconds) { flow, tracker ->
        flow.emit(true)
        advanceUntilIdle()
        flow.emit(false)
        advanceUntilIdle()
        assertEquals(ConditionEval.True, tracker.hasFluctuated())
    }

    @Test fun `(fluctuation) should delay if no fluctuation detected`() = runTrackerTest(initial = true, retention = 500.milliseconds) { _, tracker ->
        assertTrue(tracker.hasFluctuated() is ConditionEval.Delayed)
    }

    @Test fun `(fluctuation) should detect heavy rapid flips`() = runTrackerTest(initial = false, retention = 2.seconds) { flow, tracker ->
        repeat(500) {
            flow.emit(!flow.value)
            advanceUntilIdle()
        }
        assertEquals(ConditionEval.True, tracker.hasFluctuated())
    }

    // ----------------------------------------------------------------------------------------------------
    // Edge Cases
    // ----------------------------------------------------------------------------------------------------

    @Test fun `(edge) should correctly count open true crossing windowStart`() = runTrackerTest(initial = true, retention = 1.seconds) { flow, tracker ->
        Thread.sleep(600)
        advanceUntilIdle()
        flow.emit(false)
        advanceUntilIdle()
        assertEquals(ConditionEval.True, tracker.hasPersistedForAtLeast(500.milliseconds))
    }

    @Test fun `(edge) should correctly handle false before windowStart`() = runTrackerTest(initial = false, retention = 1.seconds) { flow, tracker ->
        flow.emit(true)
        advanceUntilIdle()
        Thread.sleep(400)
        flow.emit(false)
        advanceUntilIdle()
        Thread.sleep(200)
        assertEquals(ConditionEval.True, tracker.hasPersistedForAtLeast(300.milliseconds))
    }

    // ----------------------------------------------------------------------------------------------------
    // Stability
    // ----------------------------------------------------------------------------------------------------

    @Test fun `(stability) should return true if stable for requested duration`() = runTrackerTest(initial = true, retention = 0.seconds) { _, tracker ->
        advanceUntilIdle()
        Thread.sleep(300)
        assertEquals(ConditionEval.True, tracker.hasBeenStableFor(200.milliseconds))
    }

    @Test fun `(stability) should delay if not stable long enough`() = runTrackerTest(initial = true, retention = 0.seconds) { _, tracker ->
        assertTrue(tracker.hasBeenStableFor(500.milliseconds) is ConditionEval.Delayed)
    }

    @Test fun `(stability) should reset stability after flip`() = runTrackerTest(initial = true, retention = 500.milliseconds) { flow, tracker ->
        Thread.sleep(300)
        advanceUntilIdle()
        flow.emit(false)
        advanceUntilIdle()
        assertTrue(tracker.hasBeenStableFor(500.milliseconds) is ConditionEval.Delayed)
    }

    @Test fun `(stability) should behave correctly in memoryless mode`() = runTrackerTest(initial = true, retention = Duration.ZERO) { _, tracker ->
        advanceUntilIdle()
        Thread.sleep(300)
        assertEquals(ConditionEval.True, tracker.hasBeenStableFor(200.milliseconds))
    }

    // ----------------------------------------------------------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------------------------------------------------------

    @Test fun `(lifecycle) should cancel coroutine cleanly on dispose`() = runTest {
        val flow = MutableStateFlow(false)
        val tracker = BooleanTracker(flow, 500.milliseconds) { }
        tracker.dispose()
    }
}