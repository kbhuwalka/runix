package runix.primitives.action

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import runix.runtime.internal.RuntimeScope
import runix.primitives.action.ActionResult.Success
import runix.tracing.TraceEventContextElement
import runix.tracing.events.ActionRequested
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class ActionQueueTest {
    // TraceEventContextElement must be embedded in testScope's context, not applied via withContext
    // inside runQueueTest. async {} and launch {} inherit context from the CoroutineScope they are
    // called on (testScope), not from the calling coroutine. A withContext wrapper only affects
    // direct suspend calls in the current coroutine, so child coroutines launched from test lambdas
    // would still lack the element.
    private val testDispatcher = StandardTestDispatcher()
    private val testTraceEvent = ActionRequested(parent = null, actionName = "test")
    private val testScope = TestScope(testDispatcher + TraceEventContextElement(testTraceEvent))

    @BeforeTest
    fun setup() {
        mockkObject(RuntimeScope)
        every { RuntimeScope.scope } returns testScope
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    // Helper to create a queue with default parameters
    private fun createQueue(
        name: String = "TestQueue",
        timeout: Duration = Duration.INFINITE,
        allowConcurrent: Boolean = true,
        enqueueIfRunning: Boolean = true,
        block: suspend (Int) -> ActionResult = { Success }
    ): ActionQueue<Int> = ActionQueue(name, timeout, allowConcurrent, enqueueIfRunning, block)

    // Helper to run a test that involves submitting tasks to the queue
    private suspend fun runQueueTest(
        queueBuilder: () -> ActionQueue<Int>,
        testBlock: suspend (ActionQueue<Int>) -> Unit
    ) {
        val queue = queueBuilder()
        try {
            testBlock(queue)
        } finally {
            // Always clean up the queue
            queue.cancelAll()
            testScope.advanceUntilIdle()
        }
    }

    @Test
    fun `submit returns Success when action completes normally`() = testScope.runTest {
        runQueueTest(
            queueBuilder = { createQueue(block = { Success }) }
        ) { queue ->
            val result = async { queue.submit(1) }
            advanceUntilIdle()
            assertTrue(result.await() is Success)
        }
    }

    @Test
    fun `submit returns custom action result`() = testScope.runTest {
        val customError = IllegalStateException("Custom error")
        runQueueTest(
            queueBuilder = { createQueue(block = { ActionResult.Failure(customError) }) }
        ) { queue ->
            val result = async { queue.submit(1) }
            advanceUntilIdle()
            assertTrue(result.await() is ActionResult.Failure)
            assertEquals(customError, (result.await() as ActionResult.Failure).cause)
        }
    }

    @Test
    fun `submit returns Failure when action throws exception`() = testScope.runTest {
        val thrownException = RuntimeException("Test exception")
        runQueueTest(
            queueBuilder = { createQueue(block = { throw thrownException }) }
        ) { queue ->
            val result = async { queue.submit(1) }
            advanceUntilIdle()
            val failure = result.await() as ActionResult.Failure
            assertTrue(failure.cause is RuntimeException)
            assertEquals(thrownException.message, failure.cause.message)
        }
    }

    @Test
    fun `submit returns Timeout when action exceeds timeout`() = testScope.runTest {
        runQueueTest(
            queueBuilder = {
                createQueue(
                    timeout = 100.milliseconds,
                    block = { delay(1000); Success }
                )
            }
        ) { queue ->
            val result = async { queue.submit(1) }
            advanceTimeBy(200)
            advanceUntilIdle()
            assertTrue(result.await() is ActionResult.Timeout)
            assertEquals(100.milliseconds, (result.await() as ActionResult.Timeout).after)
        }
    }

    @Test
    fun `action completes normally when finishing within timeout`() = testScope.runTest {
        runQueueTest(
            queueBuilder = {
                createQueue(
                    timeout = 500.milliseconds,
                    block = { delay(100); Success }
                )
            }
        ) { queue ->
            val result = async { queue.submit(1) }
            advanceTimeBy(200)
            advanceUntilIdle()
            assertTrue(result.await() is Success)
        }
    }

    @Test
    fun `submit returns AlreadyRunning for non-concurrent queue when already running`() = testScope.runTest {
        runQueueTest(
            queueBuilder = {
                createQueue(
                    allowConcurrent = false,
                    enqueueIfRunning = false,
                    block = { delay(1000); Success }
                )
            }
        ) { queue ->
            // Start a long-running action
            val firstJob = launch { queue.submit(1) }
            advanceTimeBy(10)

            // Second submission should fail with AlreadyRunning
            val secondResult = queue.submit(2)
            assertEquals(ActionResult.AlreadyRunning, secondResult)

            advanceUntilIdle()
            firstJob.join()
        }
    }

    @Test
    fun `submit enqueues tasks for non-concurrent queue when enqueueIfRunning is true`() = testScope.runTest {
        val executionOrder = mutableListOf<Int>()

        runQueueTest(
            queueBuilder = {
                createQueue(
                    allowConcurrent = false,
                    enqueueIfRunning = true,
                    block = { param ->
                        executionOrder.add(param)
                        delay(100)
                        Success
                    }
                )
            }
        ) { queue ->
            // Submit multiple tasks in sequence
            val inputOrder = listOf(5, 2, 9)
            val jobs = inputOrder.map { value ->
                launch { queue.submit(value) }
            }

            advanceUntilIdle()
            jobs.forEach { it.join() }

            // Verify execution order matches input order
            assertEquals(inputOrder, executionOrder)
        }
    }

    @Test
    fun `concurrent tasks execute in parallel with allowConcurrent=true`() = testScope.runTest {
        val runningCount = AtomicInteger(0)
        val maxConcurrent = AtomicInteger(0)

        runQueueTest(
            queueBuilder = {
                createQueue(
                    allowConcurrent = true,
                    block = {
                        runningCount.incrementAndGet()
                        maxConcurrent.set(maxOf(maxConcurrent.get(), runningCount.get()))
                        delay(100)
                        runningCount.decrementAndGet()
                        Success
                    }
                )
            }
        ) { queue ->
            // Run multiple tasks concurrently
            val jobs = List(5) { launch { queue.submit(it) } }

            advanceUntilIdle()
            jobs.forEach { it.join() }

            // Verify multiple were running at once
            assertTrue(maxConcurrent.get() > 1, "Expected concurrent execution, max count was ${maxConcurrent.get()}")
        }
    }

    @Test
    fun `cancelRunning completes running jobs with Cancelled result`() = testScope.runTest {
        runQueueTest(
            queueBuilder = {
                createQueue(
                    block = { delay(1000); Success }
                )
            }
        ) { queue ->
            val resultDeferred = CompletableDeferred<ActionResult>()

            launch {
                val result = queue.submit(1)
                resultDeferred.complete(result)
            }

            advanceTimeBy(10)
            assertTrue(queue.isAnyJobActive())

            queue.cancelRunning()
            advanceUntilIdle()

            assertTrue(resultDeferred.isCompleted)
            assertTrue(resultDeferred.await() is ActionResult.Cancelled)
            assertFalse(queue.isAnyJobActive())
        }
    }

    @Test
    fun `cancelAll cancels both running and queued jobs`() = testScope.runTest {
        runQueueTest(
            queueBuilder = {
                createQueue(
                    allowConcurrent = false,
                    enqueueIfRunning = true,
                    block = { delay(1000); Success }
                )
            }
        ) { queue ->
            // Create multiple deferred results
            val results = List(3) { CompletableDeferred<ActionResult>() }

            // Submit tasks in order
            results.forEachIndexed { idx, deferred ->
                launch {
                    val r = queue.submit(idx)
                    deferred.complete(r)
                }
            }

            advanceTimeBy(10)
            queue.cancelAll()
            advanceUntilIdle()

            // All tasks should complete with Cancelled
            results.forEach { deferred ->
                assertTrue(deferred.isCompleted)
                assertTrue(deferred.await() is ActionResult.Cancelled)
            }

            assertFalse(queue.isAnyJobActive())
        }
    }

    @Test
    fun `action can handle cancellation of its coroutine context`() = testScope.runTest {
        val actionFinished = AtomicBoolean(false)

        runQueueTest(
            queueBuilder = {
                createQueue(
                    block = {
                        try {
                            delay(1000)
                            Success
                        } finally {
                            actionFinished.set(true)
                        }
                    }
                )
            }
        ) { queue ->
            val job = launch { queue.submit(1) }
            advanceTimeBy(10)

            job.cancel()
            advanceUntilIdle()

            // We can check actionFinished directly since it's declared outside the closure
            assertTrue(actionFinished.get(), "Action's finally block should execute on cancellation")
            assertFalse(queue.isAnyJobActive())
        }
    }

    @Test
    fun `worker restarts after channel exhaustion`() = testScope.runTest {
        val callCount = AtomicInteger(0)

        runQueueTest(
            queueBuilder = {
                createQueue(
                    block = {
                        callCount.incrementAndGet()
                        Success
                    }
                )
            }
        ) { queue ->
            // Submit first task
            queue.submit(1)
            advanceUntilIdle()

            // Wait a bit for worker to potentially exit
            advanceTimeBy(1000)

            // Submit another task - worker should restart
            queue.submit(2)
            advanceUntilIdle()

            assertEquals(2, callCount.get(), "Worker should process both tasks")
        }
    }

    @Test
    fun `queue properly handles action that throws CancellationException`() = testScope.runTest {
        runQueueTest(
            queueBuilder = {
                createQueue(
                    block = { throw CancellationException("Test cancellation") }
                )
            }
        ) { queue ->
            val result = queue.submit(1)
            advanceUntilIdle()

            assertTrue(result is ActionResult.Cancelled)
        }
    }

    @Test
    fun `non-concurrent queue processes tasks sequentially and in order`() = testScope.runTest {
        val executionOrder = mutableListOf<Int>()

        runQueueTest(
            queueBuilder = {
                createQueue(
                    allowConcurrent = false,
                    enqueueIfRunning = true,
                    block = { param ->
                        executionOrder.add(param)
                        delay(100)
                        Success
                    }
                )
            }
        ) { queue ->
            // Submit tasks in specific order
            val inputOrder = listOf(5, 2, 9, 1, 7)
            val jobs = inputOrder.map { value ->
                launch { queue.submit(value) }
            }

            advanceUntilIdle()
            jobs.forEach { it.join() }

            // Tasks should be executed in the order they were submitted
            assertEquals(inputOrder, executionOrder)
        }
    }

    @Test
    fun `isAnyJobActive correctly reflects job status`() = testScope.runTest {
        runQueueTest(
            queueBuilder = {
                createQueue(
                    block = { delay(1000); Success }
                )
            }
        ) { queue ->
            assertFalse(queue.isAnyJobActive())

            val job = launch { queue.submit(1) }
            advanceTimeBy(10)

            assertTrue(queue.isAnyJobActive())

            queue.cancelRunning()
            advanceUntilIdle()
            job.join()

            assertFalse(queue.isAnyJobActive())
        }
    }

    @Test
    fun `queue handles large number of concurrent tasks`() = testScope.runTest {
        val taskCount = 50
        val completedCount = AtomicInteger(0)

        runQueueTest(
            queueBuilder = {
                createQueue(
                    allowConcurrent = true,
                    block = {
                        delay(10) // Small delay to simulate work
                        completedCount.incrementAndGet()
                        Success
                    }
                )
            }
        ) { queue ->
            val jobs = List(taskCount) {
                launch { queue.submit(it) }
            }

            advanceUntilIdle()
            jobs.forEach { it.join() }

            assertEquals(taskCount, completedCount.get())
        }
    }

    @Test
    fun `queue can accept new tasks after cancelAll`() = testScope.runTest {
        runQueueTest(
            queueBuilder = { createQueue() }
        ) { queue ->
            // First cancel everything
            queue.cancelAll()
            advanceUntilIdle()

            // Then submit a new task - this should succeed if the queue is designed to be reusable
            val result = async { queue.submit(1) }
            advanceUntilIdle()

            // Check that the task completed successfully
            assertTrue(
                result.await() is Success,
                "Queue should accept new tasks after cancelAll by creating a new channel"
            )
        }
    }

    @Test
    fun `worker shuts down when queue is empty and restarts on demand`() = testScope.runTest {
        val processingEvents = mutableListOf<String>()
        val workerStates = mutableListOf<String>()

        runQueueTest(
            queueBuilder = {
                createQueue(
                    name = "TestIdleShutdownQueue",
                    allowConcurrent = false,
                    block = { value ->
                        processingEvents.add("Processing $value")
                        delay(100)
                        Success
                    }
                )
            }
        ) { queue ->
            // Function to check both worker and sync state
            fun recordWorkerState(label: String) {
                val isActive = queue.isWorkerActive() || queue.isAnyJobActive()
                workerStates.add("$label: ${if (isActive) "Active" else "Inactive"}")
            }

            // Initially no worker should be active
            recordWorkerState("Initial")

            // Submit first task but don't complete it yet
            launch { queue.submit(1) }
            // Advance time just enough to start processing but not complete
            advanceTimeBy(50)
            recordWorkerState("During task 1")

            // Now let the task complete
            advanceTimeBy(100)
            advanceUntilIdle()
            recordWorkerState("After task 1")

            // Submit second task
            launch { queue.submit(2) }
            // Check during processing
            advanceTimeBy(50)
            recordWorkerState("During task 2")

            // Complete the task
            advanceTimeBy(100)
            advanceUntilIdle()
            recordWorkerState("After task 2")

            // Print for debugging
            workerStates.forEach { println(it) }

            // Verify correct states - in synchronous mode, the queue sets isRunningSynchronously flag
            assertTrue(
                workerStates.filter { it.contains("During") }.all { it.contains("Active") },
                "Queue should be active during task processing. States: $workerStates"
            )

            assertTrue(
                workerStates.filter { it.contains("After") }.all { it.contains("Inactive") },
                "Queue should be inactive between tasks. States: $workerStates"
            )

            // Verify processing order
            assertEquals(listOf("Processing 1", "Processing 2"), processingEvents)
        }
    }
}
