package runix.temporal.trackers

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import runix.temporal.condition.ConditionEval
import runix.temporal.time.TestSchedulerTimeProvider
import runix.temporal.time.Time
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class NumericTrackerTest {
    private val scheduler = TestCoroutineScheduler()
    private val provider = TestSchedulerTimeProvider(scheduler)

    @BeforeTest
    fun setup() {
        Time.setProvider(provider)
    }

    @AfterTest
    fun tearDown() {
        Time.resetToRealTime()
    }

// --- Latest Persistence ---

    @Test
    fun `evaluateLatestPersisted returns True when value held long enough`() = runTest(scheduler) {
        val flow = MutableStateFlow(5.0)
        val tracker = NumericTracker(flow, 10.seconds, this)
        advanceUntilIdle()

        advanceTimeBy(5.seconds)
        val result = tracker.evaluateLatestPersisted({ it > 4.0 }, 5.seconds)
        tracker.stop()

        assertEquals(ConditionEval.True, result)
    }

    @Test
    fun `evaluateLatestPersisted returns Delayed when dwell time not yet satisfied`() = runTest(scheduler) {
        val flow = MutableStateFlow(6.0)
        val tracker = NumericTracker(flow, 10.seconds, this)
        advanceUntilIdle()

        advanceTimeBy(2.seconds)
        val result = tracker.evaluateLatestPersisted({ it > 5.0 }, 5.seconds)
        tracker.stop()

        assertTrue(result is ConditionEval.Delayed)
    }

    @Test
    fun `evaluateLatestPersisted returns False when predicate does not match`() = runTest(scheduler) {
        val flow = MutableStateFlow(2.0)
        val tracker = NumericTracker(flow, 10.seconds, this)
        advanceUntilIdle()

        val result = tracker.evaluateLatestPersisted({ it > 5.0 }, 1.seconds)
        tracker.stop()

        assertEquals(ConditionEval.False, result)
    }

    // --- Past Transitions ---

    @Test
    fun `evaluatePastTransition returns True when value matched for required time`() = runTest(scheduler) {
        val flow = MutableStateFlow(2.0)
        val tracker = NumericTracker(flow, 10.seconds, this)
        advanceUntilIdle()

        flow.value = 6.0
        advanceTimeBy(5.seconds)
        flow.value = 2.0
        advanceUntilIdle()

        val result = tracker.evaluatePastTransition({ it > 5.0 }, 5.seconds)
        tracker.stop()

        assertEquals(ConditionEval.True, result)
    }

    @Test
    fun `evaluatePastTransition returns Delayed when value still holding but not long enough`() = runTest(scheduler) {
        val flow = MutableStateFlow(2.0)
        val tracker = NumericTracker(flow, 10.seconds, this)
        advanceUntilIdle()

        flow.value = 7.0
        advanceTimeBy(3.seconds)

        val result = tracker.evaluatePastTransition({ it > 5.0 }, 5.seconds)
        tracker.stop()

        assertTrue(result is ConditionEval.Delayed)
    }

    @Test
    fun `evaluatePastTransition returns False when predicate never matched`() = runTest(scheduler) {
        val flow = MutableStateFlow(1.0)
        val tracker = NumericTracker(flow, 10.seconds, this)
        advanceUntilIdle()

        advanceTimeBy(5.seconds)
        val result = tracker.evaluatePastTransition({ it > 10.0 }, 5.seconds)
        tracker.stop()

        assertEquals(ConditionEval.False, result)
    }

    // --- Trend Detection ---

    @Test
    fun `evaluateTrend returns True for sustained increasing trend`() = runTest(scheduler) {
        val flow = MutableStateFlow(1.0)
        val tracker = NumericTracker(flow, 10.seconds, this)
        advanceUntilIdle()

        flow.value = 2.0
        advanceTimeBy(5.seconds)
        flow.value = 3.0
        advanceTimeBy(5.seconds)
        advanceUntilIdle()

        val result = tracker.evaluateTrend { current, previous -> current < previous }
        tracker.stop()

        assertEquals(ConditionEval.True, result)
    }

    @Test
    fun `evaluateTrend returns Delayed when trend is holding but retention not satisfied`() = runTest(scheduler) {
        val flow = MutableStateFlow(1.0)
        val tracker = NumericTracker(flow, 10.seconds, this)
        advanceUntilIdle()

        flow.value = 2.0
        advanceTimeBy(3.seconds)
        flow.value = 3.0
        advanceTimeBy(3.seconds)

        val result = tracker.evaluateTrend { current, previous -> current < previous }
        tracker.stop()

        assertTrue(result is ConditionEval.Delayed)
    }
}
