
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
class CategoricalTrackerTest {

    private val scheduler = TestCoroutineScheduler()
    private val provider = TestSchedulerTimeProvider(scheduler)

    @BeforeTest
    fun setup() {
        RuntimeScope.install(TestScope(scheduler))
        Time.setProvider(provider)
    }

    @AfterTest
    fun tearDown() {
        RuntimeScope.clear()
        Time.resetToRealTime()
    }

    enum class Mode { IDLE, CHARGING, MOVING, ERROR }

    // --- Transition Detection: evaluatePastTransition ---

    @Test
    fun `transitionedTo returns True when dwell-time is satisfied`() = runTest(scheduler) {
        val flow = MutableStateFlow(Mode.IDLE)
        val tracker = CategoricalTracker(flow, 10.seconds)
        advanceTimeBy(5.seconds)
        advanceUntilIdle()

        flow.value = Mode.CHARGING
        advanceTimeBy(5.seconds)
        flow.value = Mode.MOVING
        advanceUntilIdle()

        val result = tracker.evaluatePastTransition(
            match = { prev, curr -> prev == Mode.IDLE && curr == Mode.CHARGING },
            forTime = 5.seconds
        )

        tracker.stop()
        assertEquals(ConditionEval.True, result)
    }

    @Test
    fun `transitionedTo returns Delayed if dwell-time not yet satisfied and still in state`() = runTest(scheduler) {
        val flow = MutableStateFlow(Mode.IDLE)
        val tracker = CategoricalTracker(flow, 10.seconds)
        advanceUntilIdle()

        flow.value = Mode.CHARGING
        advanceTimeBy(2.seconds)
        val result = tracker.evaluatePastTransition(
            match = { prev, curr -> prev == Mode.IDLE && curr == Mode.CHARGING },
            forTime = 5.seconds
        )

        tracker.stop()
        assertTrue(result is ConditionEval.Delayed)
    }

    @Test
    fun `transitionedTo returns False if exited state early`() = runTest(scheduler) {
        val flow = MutableStateFlow(Mode.IDLE)
        val tracker = CategoricalTracker(flow, 10.seconds)
        advanceUntilIdle()

        flow.value = Mode.CHARGING
        advanceTimeBy(2.seconds)
        flow.value = Mode.MOVING
        advanceUntilIdle()

        val result = tracker.evaluatePastTransition(
            match = { prev, curr -> prev == Mode.IDLE && curr == Mode.CHARGING },
            forTime = 5.seconds
        )

        tracker.stop()
        assertEquals(ConditionEval.False, result)
    }

    @Test
    fun `transitionedTo returns False if no transition occurs`() = runTest(scheduler) {
        val flow = MutableStateFlow(Mode.IDLE)
        val tracker = CategoricalTracker(flow, 10.seconds)
        advanceUntilIdle()

        advanceTimeBy(5.seconds)
        val result = tracker.evaluatePastTransition(
            match = { prev, curr -> prev == Mode.IDLE && curr == Mode.CHARGING },
            forTime = 1.seconds
        )

        tracker.stop()
        assertEquals(ConditionEval.False, result)
    }

    // --- Latest Value Holding: evaluateLatestTransition ---

    @Test
    fun `latest state satisfies dwell time`() = runTest(scheduler) {
        val flow = MutableStateFlow(Mode.CHARGING)
        val tracker = CategoricalTracker(flow, 10.seconds)
        advanceUntilIdle()
        advanceTimeBy(6.seconds)

        val result = tracker.evaluateLatestPersisted(
            match = { it == Mode.CHARGING },
            forTime = 5.seconds
        )

        tracker.stop()
        assertEquals(ConditionEval.True, result)
    }

    @Test
    fun `latest state does not satisfy dwell time returns Delayed`() = runTest(scheduler) {
        val flow = MutableStateFlow(Mode.MOVING)
        val tracker = CategoricalTracker(flow, 10.seconds)
        advanceUntilIdle()
        advanceTimeBy(1.seconds)

        val result = tracker.evaluateLatestPersisted(
            match = { it == Mode.MOVING },
            forTime = 5.seconds
        )

        tracker.stop()
        assertTrue(result is ConditionEval.Delayed)
    }

    @Test
    fun `latest state mismatch returns False`() = runTest(scheduler) {
        val flow = MutableStateFlow(Mode.IDLE)
        val tracker = CategoricalTracker(flow, 10.seconds)
        advanceUntilIdle()
        advanceTimeBy(3.seconds)

        val result = tracker.evaluateLatestPersisted(
            match = { it == Mode.CHARGING },
            forTime = 2.seconds
        )

        tracker.stop()
        assertEquals(ConditionEval.False, result)
    }

    @Test
    fun `evaluatePastTransition returns True if final value matches and duration held`() = runTest(scheduler) {
        val flow = MutableStateFlow(Mode.IDLE)
        val tracker = CategoricalTracker(flow, 5.seconds)

        advanceTimeBy(6000) // advance more than forTime

        val result = tracker.evaluatePastTransition(
            match = { prev, curr -> prev == curr && prev == Mode.IDLE },
            forTime = 5.seconds
        )

        assertEquals(ConditionEval.True, result)
    }

    @Test
    fun `evaluatePastTransition returns Delayed if final value matches but duration not held`() = runTest(scheduler) {
        val flow = MutableStateFlow(Mode.IDLE)
        val tracker = CategoricalTracker(flow, 5.seconds)
        flow.value = Mode.IDLE

        advanceTimeBy(2000) // not long enough

        val result = tracker.evaluatePastTransition(
            match = { prev, curr -> prev == curr && prev == Mode.IDLE },
            forTime = 5.seconds
        )

        assertTrue(result is ConditionEval.Delayed)
    }

    // --- Sequence Matching: evaluateTransitionSequence ---

    @Test
    fun `sequence match succeeds`() = runTest(scheduler) {
        val flow = MutableStateFlow(Mode.IDLE)
        val tracker = CategoricalTracker(flow, 30.seconds)
        advanceUntilIdle()

        flow.value = Mode.CHARGING
        advanceTimeBy(1.seconds)
        flow.value = Mode.MOVING
        advanceTimeBy(1.seconds)
        flow.value = Mode.ERROR
        advanceUntilIdle()

        val result = tracker.evaluateTransitionSequence(
            listOf(Mode.IDLE, Mode.CHARGING, Mode.MOVING, Mode.ERROR)
        )

        tracker.stop()
        assertEquals(ConditionEval.True, result)
    }

    @Test
    fun `sequence match fails due to order break`() = runTest(scheduler) {
        val flow = MutableStateFlow(Mode.IDLE)
        val tracker = CategoricalTracker(flow, 30.seconds)
        advanceUntilIdle()

        flow.value = Mode.MOVING
        advanceTimeBy(1.seconds)
        flow.value = Mode.CHARGING
        advanceUntilIdle()

        val result = tracker.evaluateTransitionSequence(
            listOf(Mode.IDLE, Mode.CHARGING)
        )

        tracker.stop()
        assertEquals(ConditionEval.False, result)
    }

    @Test
    fun `sequence match fails with insufficient entries`() = runTest(scheduler) {
        val flow = MutableStateFlow(Mode.CHARGING)
        val tracker = CategoricalTracker(flow, 10.seconds)
        advanceUntilIdle()

        val result = tracker.evaluateTransitionSequence(
            listOf(Mode.IDLE, Mode.CHARGING)
        )

        tracker.stop()
        assertEquals(ConditionEval.False, result)
    }
}
