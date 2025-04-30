package runix.temporal.trackers

import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.*

class TrackerRegistryTest {

    enum class DummyState { A, B }

    @AfterTest
    fun cleanup() {
        TrackerRegistry.unregister("bool")
        TrackerRegistry.unregister("num")
        TrackerRegistry.unregister("cat")
    }

    @Test
    fun `register and retrieve BooleanTracker`() {
        val flow = MutableStateFlow(false)
        val tracker = BooleanTracker(flow, retention = kotlin.time.Duration.ZERO) {}
        TrackerRegistry.register("bool", tracker)

        val retrieved = TrackerRegistry.getBooleanTracker("bool")
        assertSame(tracker, retrieved)
    }

    @Test
    fun `register and retrieve NumericTracker`() {
        val flow = MutableStateFlow(0.0)
        val tracker = NumericTracker(flow, retention = kotlin.time.Duration.ZERO) {}
        TrackerRegistry.register("num", tracker)

        val retrieved = TrackerRegistry.getNumericTracker("num")
        assertSame(tracker, retrieved)
    }

    @Test
    fun `register and retrieve CategoricalTracker`() {
        val flow = MutableStateFlow(DummyState.A)
        val tracker = CategoricalTracker(flow, retention = kotlin.time.Duration.ZERO) {}
        TrackerRegistry.register("cat", tracker)

        val retrieved = TrackerRegistry.getCategoricalTracker("cat")
        assertSame(tracker, retrieved)
    }

    @Test
    fun `getXTracker throws for missing key`() {
        assertFailsWith<IllegalStateException> {
            TrackerRegistry.getBooleanTracker("missing")
        }

        assertFailsWith<IllegalStateException> {
            TrackerRegistry.getNumericTracker("missing")
        }

        assertFailsWith<IllegalStateException> {
            TrackerRegistry.getCategoricalTracker("missing")
        }
    }

    @Test
    fun `unregister stops and clears BooleanTracker`() {
        val tracker = object : BooleanTracker(
            flow = MutableStateFlow(false),
            retention = kotlin.time.Duration.ZERO,
            onUpdate = {}
        ) {
            var stopCalled = false
            var clearCalled = false
            override fun registerWith(key: String) {
            }

            override fun stop() { stopCalled = true }
            override fun clearHistory() { clearCalled = true }
        }

        TrackerRegistry.register("bool", tracker)
        TrackerRegistry.unregister("bool")

        assertTrue(tracker.stopCalled)
        assertTrue(tracker.clearCalled)
    }

    @Test
    fun `unregister handles unknown keys gracefully`() {
        // Should not throw
        TrackerRegistry.unregister("nonexistent")
    }
}