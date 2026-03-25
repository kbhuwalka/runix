package runix.temporal.condition

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import runix.runtime.internal.RuntimeScope
import runix.temporal.time.TestSchedulerTimeProvider
import runix.temporal.time.Time
import runix.temporal.trackers.BooleanTracker
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class BooleanConditionTypeTest {
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val provider = TestSchedulerTimeProvider(scheduler)

    @BeforeTest
    fun setup() {
        RuntimeScope.install(CoroutineScope(dispatcher))
        Time.setProvider(provider)
    }

    @AfterTest
    fun tearDown() {
        Time.resetToRealTime()
        RuntimeScope.clear()
    }

    private val flow = MutableStateFlow(false)

    @Test
    fun `IsTrue returns True when flow is true`() = runTest(scheduler) {
        flow.value = true
        val tracker = BooleanTracker(flow, retention = 0.seconds)
        val expr = IsTrue().compileExpression("test", TestTrackerProvider(tracker))
        assertEquals(ConditionEval.True, expr())

        tracker.stop()
    }

    @Test
    fun `IsTrue returns False when flow is false`() = runTest(scheduler) {
        flow.value = false
        val tracker = BooleanTracker(flow, 0.seconds)
        val expr = IsTrue().compileExpression("test", TestTrackerProvider(tracker))
        assertEquals(ConditionEval.False, expr())

        tracker.stop()
    }

    @Test
    fun `HasPersistedTrue returns Delayed until retention satisfied`() = runTest(scheduler) {
        val tracker = BooleanTracker(flow, retention = 5.seconds)
        flow.value = true
        advanceUntilIdle()

        val expr = HasPersistedTrue(5.seconds).compileExpression("test", TestTrackerProvider(tracker))

        assertTrue(expr() is ConditionEval.Delayed)

        advanceTimeBy(5_000)
        assertEquals(ConditionEval.True, expr())

        tracker.stop()
    }

    @Test
    fun `WasTrueFor returns True if true held in past`() = runTest(scheduler) {
        val tracker = BooleanTracker(flow, 10.seconds)
        val expr = WasTrueFor(forDuration = 3.seconds, inLast = 10.seconds)
            .compileExpression("test", TestTrackerProvider(tracker))

        flow.value = true
        advanceTimeBy(3_000)
        advanceUntilIdle()
        flow.value = false
        advanceUntilIdle()

        assertEquals(ConditionEval.True, expr())

        tracker.stop()
    }

    @Test
    fun `WasEverTrue returns True if signal was ever true`() = runTest(scheduler) {
        val tracker = BooleanTracker(flow, 10.seconds)
        val expr = WasEverTrue(inLast = 10.seconds)
            .compileExpression("test", TestTrackerProvider(tracker))

        flow.value = true
        advanceTimeBy(1.seconds)
        advanceUntilIdle()
        flow.value = false
        advanceUntilIdle()

        assertEquals(ConditionEval.True, expr())

        tracker.stop()
    }

    @Test
    fun `WasEverTrue returns False if signal was never true`() = runTest(scheduler) {
        val tracker = BooleanTracker(flow, 10.seconds)
        val expr = WasEverTrue(inLast = 10.seconds)
            .compileExpression("test", TestTrackerProvider(tracker))

        flow.value = false
        advanceTimeBy(3000)

        assertEquals(ConditionEval.False, expr())
    }

    @Test
    fun `HasFluctuated returns True after signal changes`() = runTest(scheduler) {
        val flow = MutableStateFlow(false)
        val tracker = BooleanTracker(flow, 10.seconds)
        val expr = HasFluctuated(inLast = 10.seconds)
            .compileExpression("test", TestTrackerProvider(tracker))

        flow.value = true
        advanceTimeBy(1.seconds)
        advanceUntilIdle()

        flow.value = false
        advanceTimeBy(1.seconds)
        advanceUntilIdle()

        assertEquals(ConditionEval.True, expr())

        tracker.stop()
    }

    @Test
    fun `HasFluctuated returns False when signal never changes`() = runTest(scheduler) {
        val tracker = BooleanTracker(flow, 10.seconds)
        val expr = HasFluctuated(inLast = 10.seconds)
            .compileExpression("test", TestTrackerProvider(tracker))

        flow.value = false
        advanceTimeBy(10.seconds)
        advanceUntilIdle()

        assertEquals(ConditionEval.False, expr())

        tracker.stop()
    }
}
