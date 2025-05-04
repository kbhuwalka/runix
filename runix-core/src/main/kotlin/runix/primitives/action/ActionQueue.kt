package runix.primitives.action

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import runix.runtime.internal.RuntimeScope
import java.nio.channels.ClosedChannelException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration

/**
 * A queue for processing actions with configurable concurrency and timeout settings.
 *
 * ActionQueue provides:
 * - On-demand worker lifecycle (starts when needed, shuts down when idle)
 * - Configurable concurrency behavior (sequential or parallel execution)
 * - Task timeout support
 * - Cancellation handling for both individual tasks and the entire queue
 *
 * @param T The type of data processed by the queue
 * @param name Name of the queue, used for logging and error messages
 * @param timeout Maximum time allowed for an action to complete before timing out
 * @param allowConcurrent Whether to allow concurrent processing of multiple items
 * @param enqueueIfRunning When not allowing concurrency, whether to queue additional items
 * @param block The action function that processes each item
 */
internal class ActionQueue<T>(
    private val name: String,
    private val timeout: Duration,
    private val allowConcurrent: Boolean,
    private val enqueueIfRunning: Boolean,
    private val block: suspend (T) -> ActionResult
) {
    private val channel = Channel<PendingActionExecution<T>>(Channel.UNLIMITED)
    private val runningJobs = ConcurrentHashMap<Job, PendingActionExecution<T>>()

    @Volatile
    private var workerJob: Job? = null
    private val lock = Mutex()
    private val isRunningSynchronously = AtomicBoolean(false)

    /**
     * Submits an item for processing by the queue.
     *
     * This method:
     * - Creates a deferred result for the caller to await
     * - Enqueues the item for processing
     * - Starts the worker if not already running
     * - Handles various error conditions (cancellation, queue closed, etc.)
     *
     * @param data The item to be processed
     * @return An ActionResult representing the outcome of processing
     */

    suspend fun submit(data: T): ActionResult {
        if (!allowConcurrent && !enqueueIfRunning && isRunningSynchronously.get()) {
            return ActionResult.AlreadyRunning
        }

        val deferred = CompletableDeferred<ActionResult>()
        try {
            channel.send(PendingActionExecution(data, deferred))
            maybeStartWorker()
            return deferred.await()
        } catch (e: ClosedChannelException) {
            return ActionResult.Failure(IllegalStateException("Received request to cancel all $name actions.", e))
        } catch (e: CancellationException) {
            // If the coroutine is cancelled while waiting, ensure we clean up
            if (!deferred.isCompleted) {
                deferred.complete(ActionResult.Cancelled)
            }
        } catch (e: Exception) {
            deferred.complete(ActionResult.Failure(e))
        }
        return deferred.await()
    }

    /**
     * Ensures the worker is launched exactly once.
     */
    private suspend fun maybeStartWorker() {
        if (isWorkerActive()) return

        lock.withLock {
            if (isWorkerActive()) return

            workerJob = RuntimeScope.scope.launch {
                processQueue()
            }
        }
    }

    /**
     * Processes the queue until empty.
     * Automatically exits once the queue is drained and no jobs are active.
     */
    private suspend fun processQueue() {
        try {
            // Process items until the queue is empty
            while (true) {
                // Try to get an item from the channel
                val execution = channel.tryReceive().getOrNull() ?: break

                // Process the item
                if (!allowConcurrent) {
                    isRunningSynchronously.set(true)
                    processExecution(execution)
                    isRunningSynchronously.set(false)
                } else {
                    // Launch concurrent execution
                    RuntimeScope.scope.launch {
                        processExecution(execution)
                    }
                }
            }
        } catch (e: CancellationException) {
            // Worker was cancelled - complete any pending executions with cancelled status
            drainAndCancelPending()
            throw e
        } finally {
            // Reset state when worker terminates
            isRunningSynchronously.set(false)
            workerJob = null
        }
    }

    private suspend fun processExecution(execution: PendingActionExecution<T>) {
        val executionJob = coroutineContext.job

        try {
            // Track this job as running
            runningJobs[executionJob] = execution

            // Set up timeout if needed
            val result = if (timeout.isFinite()) {
                withTimeout(timeout) {
                    block(execution.data)
                }
            } else {
                block(execution.data)
            }

            // Complete with a result if not already completed (e.g., by cancellation)
            if (!execution.deferred.isCompleted) {
                execution.deferred.complete(result)
            }
        } catch (_: TimeoutCancellationException) {
            execution.deferred.complete(ActionResult.Timeout(timeout))
        } catch (e: CancellationException) {
            // Handle cancellation by completing with a cancelled result
            if (!execution.deferred.isCompleted) {
                execution.deferred.complete(ActionResult.Cancelled)
            }
            throw e
        } catch (e: Exception) {
            // Handle other exceptions
            if (!execution.deferred.isCompleted) {
                execution.deferred.complete(ActionResult.Failure(e))
            }
        } finally {
            // Always remove from running jobs
            runningJobs.remove(executionJob)
        }
    }

    /**
     * Cancels the currently running jobs.
     */
    fun cancelRunning() {
        // ConcurrentHashMap supports concurrent iteration safely
        val jobsToCancel = runningJobs.entries.toList()
        runningJobs.clear()

        // Cancel all jobs and complete their deferreds
        jobsToCancel.forEach { (job, execution) ->
            // Only complete if not already completed
            if (!execution.deferred.isCompleted) {
                execution.deferred.complete(ActionResult.Cancelled)
            }

            job.cancel()
        }
    }

    /**
     * Cancels all jobs associated with this queue including:
     * - Currently running jobs
     * - Pending jobs in the queue
     * - The worker coroutine itself
     *
     * All pending and running operations will receive ActionResult.Cancelled.
     */
    fun cancelAll() {
        cancelRunning()

        // Drain and cancel pending tasks
        drainAndCancelPending()

        // Cancel the worker job to ensure the loop terminates
        workerJob?.cancel()
        workerJob = null
    }

    /**
     * Drains the queue and cancels all pending executions.
     */
    private fun drainAndCancelPending() {
        // Drain any pending items
        val pendingItems = mutableListOf<PendingActionExecution<T>>()
        while (true) {
            val item = channel.tryReceive().getOrNull() ?: break
            pendingItems.add(item)
        }

        // Complete all pending jobs with Cancelled status
        pendingItems.forEach {
            it.deferred.complete(ActionResult.Cancelled)
        }
    }

    /**
     * Returns true if the worker coroutine is currently active.
     */
    internal fun isWorkerActive(): Boolean = workerJob?.isActive == true

    /**
     * Returns true if the queue is currently processing any jobs.
     * This includes both concurrent jobs and synchronous processing.
     */
    fun isAnyJobActive(): Boolean = runningJobs.isNotEmpty() || isRunningSynchronously.get()
}
