package runix.temporal.condition

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import runix.runtime.internal.RuntimeScope
import runix.temporal.time.TestSchedulerTimeProvider
import runix.temporal.time.Time
import runix.temporal.trackers.NumericTracker
import runix.temporal.trackers.TrackerProvider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class NumericConditionTypeTest {

    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val provider = TestSchedulerTimeProvider(scheduler)

    @BeforeTest
    fun setup() {
        RuntimeScope.overrideScopeForTesting(CoroutineScope(dispatcher))
        Time.setProvider(provider)
    }

    @AfterTest
    fun tearDown() {
        Time.resetToRealTime()
    }

    private class TestProvider(val tracker: NumericTracker) : TrackerProvider {
        override fun getNumericTracker(key: String): NumericTracker = tracker
        override fun getBooleanTracker(key: String) = error("Not used")
        override fun getCategoricalTracker(key: String) = error("Not used")
    }

    @Test
    fun `IsAbove returns True when value is above threshold`() = runTest(scheduler) {
        val flow = MutableStateFlow(100.0)
        val tracker = NumericTracker(flow, 0.seconds) {}
        val expr = IsAbove(80.0).compileExpression("above", TestProvider(tracker))
        assertEquals(ConditionEval.True, expr())
    }

    @Test
    fun `IsBelow returns True when value is below threshold`() = runTest(scheduler) {
        val flow = MutableStateFlow(30.0)
        val tracker = NumericTracker(flow, 0.seconds) {}
        val expr = IsBelow(50.0).compileExpression("below", TestProvider(tracker))
        assertEquals(ConditionEval.True, expr())
    }

    @Test
    fun `HasPersistedAbove returns Delayed until duration is satisfied`() = runTest(scheduler) {
        val flow = MutableStateFlow(90.0)
        val tracker = NumericTracker(flow, 5.seconds) {}
        val expr = HasPersistedAbove(80.0, 5.seconds).compileExpression("persisted", TestProvider(tracker))

        assertTrue(expr() is ConditionEval.Delayed)
        advanceTimeBy(5_000)
        assertEquals(ConditionEval.True, expr())
    }

    @Test
    fun `WasAboveFor returns True when condition held in past`() = runTest(scheduler) {
        val flow = MutableStateFlow(100.0)
        val tracker = NumericTracker(flow, 10.seconds) {}
        val expr = WasAboveFor(80.0, forDuration = 3.seconds, inLast = 10.seconds)
            .compileExpression("aboveFor", TestProvider(tracker))

        advanceTimeBy(3_000)
        flow.value = 20.0
        advanceTimeBy(1000)
        assertEquals(ConditionEval.True, expr())
    }

    @Test
    fun `WasEverAbove returns True when value was ever above threshold`() = runTest(scheduler) {
        val flow = MutableStateFlow(60.0)
        val tracker = NumericTracker(flow, 10.seconds) {}
        val expr = WasEverAbove(50.0, 10.seconds).compileExpression("everAbove", TestProvider(tracker))

        flow.value = 100.0
        advanceTimeBy(1000)
        flow.value = 40.0
        advanceTimeBy(1000)

        assertEquals(ConditionEval.True, expr())
    }

    @Test
    fun `Increasing returns True for increasing values over retention`() = runTest(scheduler) {
        val flow = MutableStateFlow(10.0)
        val tracker = NumericTracker(flow, 5.seconds) {}
        val expr = Increasing(5.seconds).compileExpression("increasing", TestProvider(tracker))

        flow.value = 20.0
        advanceTimeBy(2.seconds)
        advanceUntilIdle()

        flow.value = 30.0
        advanceTimeBy(3.seconds)
        advanceUntilIdle()

        assertEquals(ConditionEval.True, expr())
    }

    @Test
    fun `StableWithin returns True when spread is under margin for duration`() = runTest(scheduler) {
        val flow = MutableStateFlow(50.0)
        val tracker = NumericTracker(flow, 5.seconds) {}
        val expr = StableWithin(margin = 1.0, retention = 5.seconds)
            .compileExpression("stable", TestProvider(tracker))

        flow.value = 50.5
        advanceTimeBy(2_000)
        flow.value = 49.8
        advanceTimeBy(3_000)

        assertEquals(ConditionEval.True, expr())
    }

    @Test
    fun `FluctuatedBeyond returns True if spread exceeds margin`() = runTest(scheduler) {
        val flow = MutableStateFlow(100.0)
        val tracker = NumericTracker(flow, 5.seconds) {}
        val expr = FluctuatedBeyond(margin = 2.0, retention = 5.seconds)
            .compileExpression("fluctuated", TestProvider(tracker))

        flow.value = 90.0
        advanceTimeBy(1_000)
        advanceUntilIdle()
        flow.value = 105.0
        advanceTimeBy(1_000)
        advanceUntilIdle()

        assertEquals(ConditionEval.True, expr())
    }

    @Test
    fun `HasPersistedBelow returns True after held duration`() = runTest(scheduler) {
        val flow = MutableStateFlow(40.0)
        val tracker = NumericTracker(flow, 5.seconds) {}
        val expr = HasPersistedBelow(50.0, 5.seconds).compileExpression("below", TestProvider(tracker))

        advanceTimeBy(5000)
        advanceUntilIdle()

        assertEquals(ConditionEval.True, expr())
    }

    @Test
    fun `HasPersistedBelow returns Delayed if not held long enough`() = runTest(scheduler) {
        val flow = MutableStateFlow(40.0)
        val tracker = NumericTracker(flow, 5.seconds) {}
        val expr = HasPersistedBelow(50.0, 5.seconds).compileExpression("below", TestProvider(tracker))

        advanceTimeBy(2000)
        advanceUntilIdle()

        assertTrue(expr() is ConditionEval.Delayed)
    }

    @Test
    fun `WasEverBelow returns True if value ever dropped below threshold`() = runTest(scheduler) {
        val flow = MutableStateFlow(100.0)
        val tracker = NumericTracker(flow, 10.seconds) {}
        val expr = WasEverBelow(80.0, 10.seconds).compileExpression("everBelow", TestProvider(tracker))

        flow.value = 75.0
        advanceTimeBy(1000)
        flow.value = 90.0
        advanceTimeBy(1000)

        assertEquals(ConditionEval.True, expr())
    }

    @Test
    fun `WasEverBelow returns False if value never dropped below threshold`() = runTest(scheduler) {
        val flow = MutableStateFlow(100.0)
        val tracker = NumericTracker(flow, 10.seconds) {}
        val expr = WasEverBelow(80.0, 10.seconds).compileExpression("everBelow", TestProvider(tracker))

        flow.value = 90.0
        advanceTimeBy(2000)

        assertEquals(ConditionEval.False, expr())
    }

    @Test
    fun `WasBelowFor returns True if value held below threshold for duration`() = runTest(scheduler) {
        val flow = MutableStateFlow(60.0)
        val tracker = NumericTracker(flow, 10.seconds) {}
        val expr = WasBelowFor(80.0, forDuration = 3.seconds, inLast = 10.seconds)
            .compileExpression("belowFor", TestProvider(tracker))

        flow.value = 70.0
        advanceTimeBy(3000)
        flow.value = 85.0
        advanceTimeBy(1000)

        assertEquals(ConditionEval.True, expr())
    }

    @Test
    fun `WasBelowFor returns Delayed if window not yet long enough`() = runTest(scheduler) {
        val flow = MutableStateFlow(60.0)
        val tracker = NumericTracker(flow, 10.seconds) {}
        val expr = WasBelowFor(80.0, forDuration = 5.seconds, inLast = 10.seconds)
            .compileExpression("belowFor", TestProvider(tracker))

        flow.value = 70.0
        advanceTimeBy(2000)
        assertTrue(expr() is ConditionEval.Delayed)
    }

    @Test
    fun `Decreasing returns True when values decrease over time`() = runTest(scheduler) {
        val flow = MutableStateFlow(100.0)
        val tracker = NumericTracker(flow, 5.seconds) {}
        val expr = Decreasing(5.seconds).compileExpression("decreasing", TestProvider(tracker))

        flow.value = 80.0
        advanceTimeBy(2000)
        flow.value = 60.0
        advanceTimeBy(3000)

        assertEquals(ConditionEval.True, expr())
    }

    @Test
    fun `Decreasing returns Delayed if trend not yet long enough`() = runTest(scheduler) {
        val flow = MutableStateFlow(100.0)
        val tracker = NumericTracker(flow, 5.seconds) {}
        val expr = Decreasing(5.seconds).compileExpression("decreasing", TestProvider(tracker))

        flow.value = 90.0
        advanceTimeBy(2000)

        assertTrue(expr() is ConditionEval.Delayed)
    }
}
