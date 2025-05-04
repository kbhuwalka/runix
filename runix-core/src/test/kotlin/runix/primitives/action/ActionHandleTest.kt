package runix.primitives.action

import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import runix.primitives.module.AppModule
import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ActionHandleTest {
    private val testScope = TestScope()
    
    private fun createTestModule(name: String = "TestModule") = mockk<AppModule> {
        every { this@mockk.name } returns name
    }
    
    // Helper function to create a test action with default values
    private fun <T> createAction(
        name: String = "TestAction",
        allowConcurrent: Boolean = false,
        enqueueIfRunning: Boolean = false,
        timeout: Duration = Duration.INFINITE,
        block: suspend (T) -> ActionResult = { ActionResult.Success }
    ): ActionHandle<T> = ActionHandle(name, allowConcurrent, enqueueIfRunning, timeout, block)
    
    // Helper to inject a mock queue into an action for testing
    private fun <T> ActionHandle<T>.injectMockQueue(mockQueue: ActionQueue<T>) {
        val queueField = ActionHandle::class.java.getDeclaredField("queue")
        queueField.isAccessible = true
        queueField.set(this, mockQueue)
    }

    @Test
    fun `constructor properly initializes with correct parameters`() = testScope.runTest {
        val action = createAction<String>(
            name = "TestAction",
            allowConcurrent = true,
            enqueueIfRunning = true,
            timeout = 5.seconds
        )
        
        assertEquals("TestAction", action.name)
        assertEquals("Action(TestAction)", action.toString())
        assertFalse(action.isRunning())
    }
    
    @Test
    fun `register method updates registration state`() {
        val action = createAction<String>()
        val module = createTestModule()
        
        // Should not throw exception
        action.register(module)
        
        // Second registration should throw
        assertFailsWith<IllegalStateException> {
            action.register(module)
        }
    }
    
    @Test
    fun `run delegates to queue submit and returns result`() = testScope.runTest {
        val mockQueue = mockk<ActionQueue<String>>()
        val expectedResult = ActionResult.Success
        coEvery { mockQueue.submit(any()) } returns expectedResult
        
        val action = createAction<String>()
        action.injectMockQueue(mockQueue)
        
        val result = action.run("test-data")
        
        assertEquals(expectedResult, result)
        coVerify { mockQueue.submit("test-data") }
    }
    
    @Test
    fun `isRunning delegates to queue isAnyJobActive`() {
        val mockQueue = mockk<ActionQueue<String>>()
        every { mockQueue.isAnyJobActive() } returns true
        
        val action = createAction<String>()
        action.injectMockQueue(mockQueue)
        
        assertTrue(action.isRunning())
        verify { mockQueue.isAnyJobActive() }
        
        // Change mock behavior and test again
        every { mockQueue.isAnyJobActive() } returns false
        assertFalse(action.isRunning())
    }
    
    @Test
    fun `cancel delegates to queue cancelRunning`() {
        val mockQueue = mockk<ActionQueue<String>>()
        every { mockQueue.cancelRunning() } just Runs
        
        val action = createAction<String>()
        action.injectMockQueue(mockQueue)
        
        action.cancel()
        verify { mockQueue.cancelRunning() }
    }
    
    @Test
    fun `cancelAll delegates to queue cancelAll`() = testScope.runTest {
        val mockQueue = mockk<ActionQueue<String>>()
        coEvery { mockQueue.cancelAll() } just Runs
        
        val action = createAction<String>()
        action.injectMockQueue(mockQueue)
        
        action.cancelAll()
        coVerify { mockQueue.cancelAll() }
    }
    
    @Test
    fun `integration test - running action works end to end`() = testScope.runTest {
        var executionCount = 0
        val action = createAction<String> {
            executionCount++
            ActionResult.Success
        }
        
        val module = createTestModule()
        action.register(module)
        
        val result = action.run("test-data")
        
        assertEquals(1, executionCount)
        assertEquals(ActionResult.Success, result)
    }
    
    @Test
    fun `concurrency and queueing policies work as expected`() = testScope.runTest {
        // Case 1: Non-concurrent, non-queueing
        val nonConcurrentAction = createAction<Unit>(
            allowConcurrent = false,
            enqueueIfRunning = false
        )
        
        // Mock the queue to simulate already running
        val mockQueue = mockk<ActionQueue<Unit>>()
        every { mockQueue.isAnyJobActive() } returns true
        coEvery { mockQueue.submit(Unit) } returns ActionResult.AlreadyRunning
        nonConcurrentAction.injectMockQueue(mockQueue)
        
        assertEquals(ActionResult.AlreadyRunning, nonConcurrentAction.run(Unit))
        
        // Case 2: Non-concurrent, queueing
        val queueingAction = createAction<Unit>(
            allowConcurrent = false,
            enqueueIfRunning = true
        )
        
        val mockQueue2 = mockk<ActionQueue<Unit>>()
        every { mockQueue2.isAnyJobActive() } returns true
        coEvery { mockQueue2.submit(Unit) } returns ActionResult.Success
        queueingAction.injectMockQueue(mockQueue2)
        
        assertEquals(ActionResult.Success, queueingAction.run(Unit))
        coVerify { mockQueue2.submit(Unit) }
    }
    
    @Test
    fun `timeout works as expected`() = testScope.runTest {
        // Create a mock queue that simulates a timeout
        val mockQueue = mockk<ActionQueue<Unit>>()
        coEvery { mockQueue.submit(Unit) } returns ActionResult.Timeout(1.seconds)
        
        val action = createAction<Unit>(timeout = 1.seconds)
        action.injectMockQueue(mockQueue)
        
        val result = action.run(Unit)
        assertTrue(result is ActionResult.Timeout)
    }
}