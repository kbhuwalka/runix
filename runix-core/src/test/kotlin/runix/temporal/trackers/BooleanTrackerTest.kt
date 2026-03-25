
package runix.temporal.trackers

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import runix.runtime.internal.RuntimeScope
import runix.temporal.condition.ConditionEval
import runix.temporal.time.TestSchedulerTimeProvider
import runix.temporal.time.Time
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class BooleanTrackerTest {

    private val scheduler = TestCoroutineScheduler()
    private val testScope = TestScope(scheduler)
    private val provider = TestSchedulerTimeProvider(scheduler)

    @BeforeTest
    fun setup() {
        Time.setProvider(provider)
        RuntimeScope.install(testScope)
    }

    @AfterTest
    fun tearDown() {
        RuntimeScope.clear()
        Time.resetToRealTime()
    }

    // --- Latest Persistence ---

    @Test
    fun `evaluateLatestPersisted returns True when value has been held long enough`() = runTest(scheduler) {
        val flow = MutableStateFlow(true)
        val tracker = BooleanTracker(flow, 10.seconds)
        advanceUntilIdle()

        advanceTimeBy(5.seconds)
        val result = tracker.evaluateLatestPersisted({ it }, 5.seconds)

        tracker.stop()
        assertEquals(ConditionEval.True, result)
    }

    @Test
    fun `evaluateLatestPersisted returns Delayed when dwell time not yet satisfied`() = runTest(scheduler) {
        val flow = MutableStateFlow(true)
        val tracker = BooleanTracker(flow, 10.seconds)
        advanceUntilIdle()

        advanceTimeBy(2.seconds)
        val result = tracker.evaluateLatestPersisted({ it }, 5.seconds)

        tracker.stop()
        assertTrue(result is ConditionEval.Delayed)
    }

    @Test
    fun `evaluateLatestPersisted returns False when predicate does not match current value`() = runTest(scheduler) {
        val flow = MutableStateFlow(false)
        val tracker = BooleanTracker(flow, 10.seconds)
        advanceUntilIdle()

        advanceTimeBy(2.seconds)
        val result = tracker.evaluateLatestPersisted({ it }, 1.seconds)

        tracker.stop()
        assertEquals(ConditionEval.False, result)
    }

    // --- Historical Transitions ---

    @Test
    fun `evaluatePastTransition returns True when value was held long enough`() = runTest(scheduler) {
        val flow = MutableStateFlow(false)
        val tracker = BooleanTracker(flow, 10.seconds)
        advanceUntilIdle()

        flow.value = true
        advanceTimeBy(5.seconds)
        flow.value = false
        advanceUntilIdle()

        val result = tracker.evaluatePastTransition({ it }, 5.seconds)

        tracker.stop()
        assertEquals(ConditionEval.True, result)
    }

    @Test
    fun `evaluatePastTransition returns Delayed when value is still holding but not long enough`() = runTest(scheduler) {
        val flow = MutableStateFlow(false)
        val tracker = BooleanTracker(flow, 10.seconds)
        advanceUntilIdle()

        flow.value = true
        advanceTimeBy(2.seconds)

        val result = tracker.evaluatePastTransition({ it }, 5.seconds)

        tracker.stop()
        assertTrue(result is ConditionEval.Delayed)
    }

    @Test
    fun `evaluatePastTransition returns False when value was not held long enough`() = runTest(scheduler) {
        val flow = MutableStateFlow(false)
        val tracker = BooleanTracker(flow, 10.seconds)
        advanceUntilIdle()

        flow.value = true
        advanceTimeBy(2.seconds)
        flow.value = false
        advanceUntilIdle()

        val result = tracker.evaluatePastTransition({ it }, 5.seconds)

        tracker.stop()
        assertEquals(ConditionEval.False, result)
    }

    @Test
    fun `evaluatePastTransition returns False when predicate never matched`() = runTest(scheduler) {
        val flow = MutableStateFlow(false)
        val tracker = BooleanTracker(flow, 10.seconds)
        advanceUntilIdle()

        advanceTimeBy(5.seconds)
        val result = tracker.evaluatePastTransition({ it }, 1.seconds)

        tracker.stop()
        assertEquals(ConditionEval.False, result)
    }

    @Test
    fun `evaluatePastTransition returns True when last matching value satisfies dwell`() = runTest(scheduler) {
        val flow = MutableStateFlow(false)
        val tracker = BooleanTracker(flow, 10.seconds)
        advanceUntilIdle()

        flow.value = true
        advanceTimeBy(5.seconds)

        val result = tracker.evaluatePastTransition({ it }, 5.seconds)
        tracker.stop()

        assertEquals(ConditionEval.True, result)
    }

    // --- Fluctuation Detection ---

    @Test
    fun `evaluateFluctuated returns False when only one value exists`() = runTest(scheduler) {
        val flow = MutableStateFlow(true)
        val tracker = BooleanTracker(flow, 10.seconds)
        advanceUntilIdle()

        advanceTimeBy(2.seconds)
        val result = tracker.evaluateFluctuated()

        tracker.stop()
        assertEquals(ConditionEval.False, result)
    }

    @Test
    fun `evaluateFluctuated returns True when value changed`() = runTest(scheduler) {
        val flow = MutableStateFlow(true)
        val tracker = BooleanTracker(flow, 10.seconds)
        advanceUntilIdle()

        flow.value = false
        advanceUntilIdle()

        val result = tracker.evaluateFluctuated()

        tracker.stop()
        assertEquals(ConditionEval.True, result)
    }
}
