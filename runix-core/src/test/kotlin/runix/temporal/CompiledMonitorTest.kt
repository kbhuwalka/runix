package runix.temporal

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import runix.runtime.internal.RuntimeScope
import runix.temporal.condition.ConditionEval
import runix.temporal.condition.TemporalExpression
import runix.temporal.trackers.BaseTracker
import runix.temporal.trackers.TrackerRegistry
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class CompiledMonitorTest {

    // Test coroutine scope using a test dispatcher
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    // Tracks registered keys for verification
    private var registeredKeys = mutableListOf<String>()
    
    // Flow update tracking
    private val updateCallsLog = mutableListOf<String>()
    
    // Mock implementation of BaseTracker for testing
    private inner class TestTracker<T>(
        val sourceFlow: StateFlow<T>,
        val retention: Duration = Duration.ZERO
    ) : BaseTracker<T>(sourceFlow, retention) {
        var registeredKey: String? = null
        
        override fun registerWith(key: String) {
            registeredKey = key
            registeredKeys.add(key)
        }
    }
    
    // Setup and teardown
    @BeforeEach
    fun setup() {
        // Set RuntimeScope for testing
        RuntimeScope.install(testScope)
        registeredKeys.clear()
        updateCallsLog.clear()
    }
    
    @AfterEach
    fun teardown() {
        // Cancel any running coroutines
        testScope.coroutineContext.job.cancelChildren()
        
        // Reset RuntimeScope override
        RuntimeScope.clear()
    }
    
    @Test
    fun `start registers trackers with correct keys`() = runTest(testDispatcher) {
        // Create test trackers and bindings
        val tracker1 = TestTracker(MutableStateFlow("value1"))
        val tracker2 = TestTracker(MutableStateFlow("value2"))
        
        val bindings = listOf(
            FlowBinding("key1", Duration.ZERO, tracker1.sourceFlow, tracker1),
            FlowBinding("key2", Duration.ZERO, tracker2.sourceFlow, tracker2)
        )
        
        // Create and start monitor
        val monitor = CompiledMonitor("test-monitor", { ConditionEval.True }, bindings)
        monitor.start { updateCallsLog.add("update") }
        
        // Verify trackers were registered with correct keys
        assertEquals("key1", tracker1.registeredKey)
        assertEquals("key2", tracker2.registeredKey)
        assertEquals(listOf("key1", "key2"), registeredKeys)
    }
    
    @Test
    fun `start is idempotent`() = runTest(testDispatcher) {
        // Create test tracker and binding
        val tracker = TestTracker(MutableStateFlow("value"))
        val binding = FlowBinding("test-key", Duration.ZERO, tracker.sourceFlow, tracker)
        
        // Create monitor
        val monitor = CompiledMonitor("test-monitor", { ConditionEval.True }, listOf(binding))
        
        // Start multiple times
        monitor.start { updateCallsLog.add("update") }
        monitor.start { updateCallsLog.add("update") }
        monitor.start { updateCallsLog.add("update") }
        
        // Verify tracker was registered exactly once
        assertEquals(1, registeredKeys.size)
        assertEquals("test-key", tracker.registeredKey)
    }
    
    @Test
    fun `flow updates trigger callback`() = runTest(testDispatcher) {
        // Create a mutable flow we can update
        val flow = MutableStateFlow("initial")
        val tracker = TestTracker(flow)
        val binding = FlowBinding("test-key", Duration.ZERO, flow, tracker)
        
        // Create and start monitor
        val monitor = CompiledMonitor("test-monitor", { ConditionEval.True }, listOf(binding))
        monitor.start { updateCallsLog.add("update-${flow.value}") }
        
        // Update the flow and advance test time
        flow.value = "updated"
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify callback was triggered
        assertTrue(updateCallsLog.contains("update-updated"))
    }
    
    @Test
    fun `duplicate flows only trigger callback once per update`() = runTest(testDispatcher) {
        // Create a single flow used by multiple trackers
        val sharedFlow = MutableStateFlow("shared")
        val tracker1 = TestTracker(sharedFlow)
        val tracker2 = TestTracker(sharedFlow)
        
        val bindings = listOf(
            FlowBinding("key1", Duration.ZERO, sharedFlow, tracker1),
            FlowBinding("key2", Duration.ZERO, sharedFlow, tracker2)
        )
        
        // Create and start monitor
        val monitor = CompiledMonitor("test-monitor", { ConditionEval.True }, bindings)
        monitor.start { updateCallsLog.add("update") }
        
        // Update the flow and advance test time
        sharedFlow.value = "new-value"
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify callback was triggered exactly once
        assertEquals(1, updateCallsLog.size)
    }
    
    @Test
    fun `stop unregisters trackers and cancels jobs`() = runTest(testDispatcher) {
        // Create a test tracker mock that can verify unregistration
        val tracker = TestTracker(MutableStateFlow("value"))
        val binding = FlowBinding("stop-key", Duration.ZERO, tracker.sourceFlow, tracker)
        
        // Mock the TrackerRegistry unregister method
        var unregisteredKeys = mutableListOf<String>()
        mockkObject(TrackerRegistry)
        every { TrackerRegistry.unregister(any()) } answers { 
            unregisteredKeys.add(firstArg())
        }
        
        // Create and start monitor
        val monitor = CompiledMonitor("stop-monitor", { ConditionEval.True }, listOf(binding))
        monitor.start { updateCallsLog.add("update") }
        
        // Verify monitor has active jobs
        assertTrue(testScope.coroutineContext.job.children.toList().isNotEmpty())
        
        // Stop the monitor
        monitor.stop()
        advanceUntilIdle()
        
        // Verify tracker was unregistered and jobs were cancelled
        assertEquals(listOf("stop-key"), unregisteredKeys)
        assertFalse(testScope.coroutineContext.job.children.toList().any { it.isCancelled })
        
        unmockkAll()
    }
    
    @Test
    fun `stop is idempotent`() = runTest(testDispatcher) {
        // Create tracker and binding
        val tracker = TestTracker(MutableStateFlow("value"))
        val binding = FlowBinding("test-key", Duration.ZERO, tracker.sourceFlow, tracker)
        
        // Mock the TrackerRegistry unregister method
        var unregisterCallCount = 0
        mockkObject(TrackerRegistry)
        every { TrackerRegistry.unregister(any()) } answers { 
            unregisterCallCount++
        }
        
        // Create and start monitor
        val monitor = CompiledMonitor("test-monitor", { ConditionEval.True }, listOf(binding))
        monitor.start { updateCallsLog.add("update") }
        
        // Stop multiple times
        monitor.stop()
        monitor.stop()
        monitor.stop()
        
        // Verify unregister was called exactly once per binding
        assertEquals(1, unregisterCallCount)
        
        unmockkAll()
    }
    
    @Test
    fun `condition is evaluated correctly`() = runTest(testDispatcher) {
        // Create a condition that returns a specific value
        val condition: TemporalExpression = { ConditionEval.True }
        
        // Create monitor with the condition
        val monitor = CompiledMonitor("condition-monitor", condition, emptyList())
        
        // Verify condition is evaluated correctly
        assertEquals(ConditionEval.True, monitor.condition())
    }
}