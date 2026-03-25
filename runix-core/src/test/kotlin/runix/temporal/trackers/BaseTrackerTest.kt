package runix.temporal.trackers

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import runix.runtime.internal.RuntimeScope
import runix.temporal.time.TestSchedulerTimeProvider
import runix.temporal.time.Time
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class BaseTrackerTest {
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

    /**
     * Simple concrete tracker to expose history for testing.
     */
    private class TestTracker<T>(
        flow: MutableStateFlow<T>,
        retention: Duration
    ) : BaseTracker<T>(flow, retention) {
        fun entries() = history.entries().toList()
        override fun registerWith(key: String) {}
    }

    @Test
    fun `seed on creation`() = runTest(scheduler) {
        val flow = MutableStateFlow("X")
        val tracker = TestTracker(flow, retention = 5.seconds)
        advanceUntilIdle()
        tracker.stop()

        val entries = tracker.entries()
        assertEquals(listOf("X"), entries.map { it.value })
    }

    @Test
    fun `append on update`() = runTest(scheduler) {
        val flow = MutableStateFlow(0)
        val tracker = TestTracker(flow, retention = 5.seconds)
        advanceUntilIdle()

        // advance time before new update
        advanceTimeBy(1.seconds)
        flow.value = 1
        advanceUntilIdle()
        tracker.stop()

        val values = tracker.entries().map { it.value }
        assertEquals(listOf(0, 1), values)
    }

    @Test
    fun `no-dup on same value`() = runTest(scheduler) {
        val flow = MutableStateFlow(42)
        val tracker = TestTracker(flow, retention = 5.seconds)
        advanceUntilIdle()

        advanceTimeBy(1.seconds)
        flow.value = 100
        advanceUntilIdle()

        advanceTimeBy(1.seconds)
        flow.value = 100
        advanceUntilIdle()
        tracker.stop()

        val values = tracker.entries().map { it.value }
        assertEquals(listOf(42, 100), values)
    }

    @Test
    fun `stop cancels collector and prevents further updates`() = runTest(scheduler) {
        val flow = MutableStateFlow("start")
        val tracker = TestTracker(flow, retention = 5.seconds)
        advanceUntilIdle()

        advanceTimeBy(1.seconds)
        flow.value = "mid"
        advanceUntilIdle()
        assertEquals(listOf("start", "mid"), tracker.entries().map { it.value })

        // Stop tracking
        tracker.stop()

        advanceTimeBy(1.seconds)
        flow.value = "end"
        advanceUntilIdle()
        tracker.stop()

        val valuesAfterStop = tracker.entries().map { it.value }
        assertEquals(listOf("start", "mid"), valuesAfterStop)
    }

    @Test
    fun `retention window prunes old entries`() = runTest(scheduler) {
        val flow = MutableStateFlow(0)
        val tracker = TestTracker(flow, retention = 2.seconds)
        advanceUntilIdle()

        // t = 0s, initial
        advanceTimeBy(1.seconds)
        flow.value = 1
        advanceUntilIdle()

        // t = 3s, update
        advanceTimeBy(2.seconds)
        flow.value = 2
        advanceUntilIdle()
        tracker.stop()

        // At this point, initial value at 0s is expired (windowStart=2s)
        val values = tracker.entries().map { it.value }
        assertEquals(listOf(1, 2), values)
    }
}
