package runix.temporal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import runix.temporal.condition.ConditionEval
import runix.temporal.trackers.BaseTracker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Duration

class CompiledMonitorTest {
    
    private class FakeTracker(
        var registeredKey: String? = null,
        var lastUpdateCallback: (() -> Unit)? = null
    ) : BaseTracker<Unit>(
        flow = MutableStateFlow<Unit>(Unit),
        retention = Duration.ZERO,
        scope = CoroutineScope(StandardTestDispatcher()),
        onUpdate = {}
    ) {
        override fun registerWith(key: String) {
            registeredKey = key
        }
    }

    @Test
    fun `start registers tracker and saves key`() {
        val tracker = FakeTracker()
        val binding = FlowBinding(
            key = "tracker-key",
            retention = Duration.ZERO,
            createTracker = {
                tracker.lastUpdateCallback = it
                tracker
            }
        )

        val monitor = CompiledMonitor(
            name = "test-monitor",
            condition = { ConditionEval.True },
            bindings = listOf(binding)
        )

        monitor.start { }

        assertEquals("tracker-key", tracker.registeredKey)
    }

    @Test
    fun `start passes onUpdate callback to tracker`() {
        val tracker = FakeTracker()
        val callbackLog = mutableListOf<String>()

        val binding = FlowBinding(
            key = "update-test",
            retention = Duration.ZERO,
            createTracker = {
                tracker.lastUpdateCallback = it
                tracker
            }
        )

        val monitor = CompiledMonitor("update-monitor", { ConditionEval.True }, listOf(binding))

        monitor.start { callbackLog += "onUpdate called" }
        tracker.lastUpdateCallback?.invoke()

        assertEquals(listOf("onUpdate called"), callbackLog)
    }

    @Test
    fun `start only runs tracker creation once`() {
        var createTrackerCount = 0
        val tracker = FakeTracker()

        val binding = FlowBinding(
            key = "start-once",
            retention = Duration.ZERO,
            createTracker = {
                createTrackerCount++
                tracker
            }
        )

        val monitor = CompiledMonitor("once-monitor", { ConditionEval.True }, listOf(binding))

        monitor.start {}
        monitor.start {}
        monitor.start {}

        assertEquals(1, createTrackerCount)
    }

    @Test
    fun `evaluate returns correct ConditionEval`() {
        val monitor = CompiledMonitor(
            name = "eval-monitor",
            condition = { ConditionEval.True },
            bindings = emptyList()
        )

        assertEquals(ConditionEval.True, monitor.condition())
    }

    @Test
    fun `stop is idempotent and safe`() {
        val tracker = FakeTracker()
        val binding = FlowBinding(
            key = "stop-key",
            retention = Duration.ZERO,
            createTracker = { tracker }
        )

        val monitor = CompiledMonitor("stop-monitor", { ConditionEval.True }, listOf(binding))
        monitor.start { }

        monitor.stop()
        assertNotNull(tracker.registeredKey)

        tracker.registeredKey = null // simulate internal cleanup
        monitor.stop() // shouldn't crash
    }
}