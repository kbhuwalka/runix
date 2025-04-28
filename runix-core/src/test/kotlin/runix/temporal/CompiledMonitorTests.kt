package runix.temporal

import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlin.test.BeforeTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Unit tests for CompiledMonitor.
 * Verifies that startTracking() and stopTracking() properly
 * call TemporalEngine.trackBoolean and untrackBoolean.
 */
class CompiledMonitorTests {

    private val flow1 = MutableStateFlow(true)
    private val flow2 = MutableStateFlow(false)

    @BeforeTest
    fun setup() {
        // Mock the TemporalEngine singleton
        mockkObject(TemporalEngine)
    }

    @AfterTest
    fun teardown() {
        // Clear mocks on TemporalEngine
        unmockkObject(TemporalEngine)
    }

    @Test
    fun `startTracking calls trackBoolean for each registration`() {
        val reg1 = FlowRegistration(flow1, "key1", 1.seconds)
        val reg2 = FlowRegistration(flow2, "key2", 2.seconds)
        val monitor = CompiledMonitor("testMonitor", { ConditionEval.True }, listOf(reg1, reg2))

        // Use a distinct callback to verify it is passed through
        val callback: () -> Unit = {}

        monitor.startTracking(callback)

        // Verify that trackBoolean was called with the correct args
        verify(exactly = 1) {
            TemporalEngine.trackBoolean(flow1, "key1", 1.seconds, callback)
        }
        verify(exactly = 1) {
            TemporalEngine.trackBoolean(flow2, "key2", 2.seconds, callback)
        }
    }

    @Test
    fun `stopTracking calls untrackBoolean for each key`() {
        val reg1 = FlowRegistration(flow1, "key1", 1.seconds)
        val reg2 = FlowRegistration(flow2, "key2", 2.seconds)
        val monitor = CompiledMonitor("testMonitor", { ConditionEval.True }, listOf(reg1, reg2))

        monitor.stopTracking()

        // Verify that untrackBoolean was called for each key
        verify(exactly = 1) { TemporalEngine.untrackBoolean("key1") }
        verify(exactly = 1) { TemporalEngine.untrackBoolean("key2") }
    }

    @Test
    fun `startTracking with no registrations does nothing`() {
        val monitor = CompiledMonitor("testMonitor", { ConditionEval.True }, emptyList())
        val callback: () -> Unit = {}

        monitor.startTracking(callback)

        // No trackBoolean calls should occur
        verify(exactly = 0) {
            TemporalEngine.trackBoolean(any(), any(), any(), any())
        }
    }

    @Test
    fun `stopTracking with no registrations does nothing`() {
        val monitor = CompiledMonitor("testMonitor", { ConditionEval.True }, emptyList())

        monitor.stopTracking()

        // No untrackBoolean calls should occur
        verify(exactly = 0) {
            TemporalEngine.untrackBoolean(any())
        }
    }
}