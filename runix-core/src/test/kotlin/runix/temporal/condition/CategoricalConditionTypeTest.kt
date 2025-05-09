@file:OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)

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
import runix.temporal.trackers.CategoricalTracker
import runix.temporal.trackers.TrackerProvider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

class CategoricalConditionTypeTest {

    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val provider = TestSchedulerTimeProvider(scheduler)

    enum class Mode { IDLE, RUNNING, CHARGING }

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

    private class TestProvider(val tracker: CategoricalTracker<*>) : TrackerProvider {
        override fun getBooleanTracker(key: String) = error("Not used")
        override fun getNumericTracker(key: String) = error("Not used")
        override fun getCategoricalTracker(key: String): CategoricalTracker<*> = tracker
    }

    @Test
    fun `IsInState returns True if current state matches`() = runTest(scheduler) {
        val flow = MutableStateFlow(Mode.IDLE)
        val tracker = CategoricalTracker(flow, Duration.ZERO)
        val expr = IsInState(Mode.IDLE).compileExpression("isIn", TestProvider(tracker))
        assertEquals(ConditionEval.True, expr())
    }

    @Test
    fun `PersistedInState returns True if state held for duration`() = runTest(scheduler) {
        val flow = MutableStateFlow(Mode.CHARGING)
        val tracker = CategoricalTracker(flow, 5.seconds)
        val expr = PersistedInState(Mode.CHARGING, 5.seconds).compileExpression("persisted", TestProvider(tracker))

        advanceTimeBy(5000)
        assertEquals(ConditionEval.True, expr())
    }

    @Test
    fun `WasInStateFor returns True when state held in past window`() = runTest(scheduler) {
        val flow = MutableStateFlow(Mode.RUNNING)
        val tracker = CategoricalTracker(flow, 10.seconds)
        val expr = WasInStateFor(Mode.RUNNING, forDuration = 3.seconds, inLast = 10.seconds)
            .compileExpression("wasIn", TestProvider(tracker))

        advanceTimeBy(3000)
        advanceUntilIdle()
        flow.value = Mode.IDLE
        advanceTimeBy(1000)
        advanceUntilIdle()

        assertEquals(ConditionEval.True, expr())
    }

    @Test
    fun `WasEverInState returns True if state occurred at all`() = runTest(scheduler) {
        val flow = MutableStateFlow(Mode.IDLE)
        val tracker = CategoricalTracker(flow, 10.seconds)
        val expr = WasEverInState(Mode.RUNNING, 10.seconds).compileExpression("wasEverIn", TestProvider(tracker))

        flow.value = Mode.RUNNING
        advanceTimeBy(1000)
        flow.value = Mode.IDLE
        advanceTimeBy(1000)

        assertEquals(ConditionEval.True, expr())
    }

    @Test
    fun `TransitionedTo returns True when state is entered`() = runTest(scheduler) {
        val flow = MutableStateFlow(Mode.IDLE)
        val tracker = CategoricalTracker(flow, 10.seconds)
        val expr = TransitionedTo(Mode.CHARGING, forDuration = 0.seconds, inLast = 10.seconds)
            .compileExpression("to", TestProvider(tracker))

        flow.value = Mode.CHARGING
        advanceTimeBy(1000)
        assertEquals(ConditionEval.True, expr())
    }

    @Test
    fun `TransitionedFrom returns True when state is exited`() = runTest(scheduler) {
        val flow = MutableStateFlow(Mode.CHARGING)
        val tracker = CategoricalTracker(flow, 10.seconds)
        val expr = TransitionedFrom(Mode.CHARGING, forDuration = 0.seconds, inLast = 10.seconds)
            .compileExpression("from", TestProvider(tracker))

        flow.value = Mode.IDLE
        advanceTimeBy(1000)
        assertEquals(ConditionEval.True, expr())
    }

    @Test
    fun `TransitionedThrough returns True if full sequence occurs`() = runTest(scheduler) {
        val flow = MutableStateFlow(Mode.IDLE)
        val tracker = CategoricalTracker(flow, 10.seconds)
        val expr = TransitionedThrough(listOf(Mode.IDLE, Mode.RUNNING, Mode.CHARGING), 10.seconds)
            .compileExpression("through", TestProvider(tracker))

        flow.value = Mode.RUNNING
        advanceTimeBy(1000)
        flow.value = Mode.CHARGING
        advanceTimeBy(1000)

        assertEquals(ConditionEval.True, expr())
    }
}
