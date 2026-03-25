package runix.primitives.monitor

import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import runix.runtime.internal.RuntimeScope
import runix.temporal.time.delayUntil
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.ComparableTimeMark
import kotlin.time.TimeMark

/**
 * Dispatcher that ensures only one evaluation runs at a time.
 * If additional evaluations are requested during execution,
 * they will be coalesced into a single extra run.
 *
 * Supports scheduling an evaluation at a future [TimeMark] and cancelling it.
 *
 * Concurrency model: [requestEvaluate] is safe to call from multiple coroutines concurrently.
 * A [Channel.CONFLATED] trigger collapses concurrent requests into at most one pending run.
 * An [AtomicBoolean] gate ensures exactly one worker is started at a time. On exit, the worker
 * performs a final channel check to guard against items that arrive between the last drain and
 * releasing the gate.
 */
internal class MonitorEvaluationDispatcher(
    private val evaluate: suspend () -> Unit
) {
    private val trigger = Channel<Unit>(Channel.CONFLATED)
    private val workerRunning = AtomicBoolean(false)

    @Volatile private var workerJob: Job? = null
    @Volatile private var scheduledJob: Job? = null

    /**
     * Requests an evaluation to occur as soon as possible.
     * If one is already running, the request is coalesced into a single follow-up run.
     */
    fun requestEvaluate() {
        trigger.trySend(Unit)
        maybeStartWorker()
    }

    /**
     * Schedules an evaluation to run at a future [targetTime].
     * If one is already scheduled, it will be replaced.
     * When the time is reached, [requestEvaluate] is invoked.
     */
    fun scheduleEvaluateAt(targetTime: ComparableTimeMark) {
        scheduledJob?.cancel()
        scheduledJob = RuntimeScope.scope.launch {
            delayUntil(targetTime)
            requestEvaluate()
        }
    }

    /**
     * Cancels any scheduled (but not yet run) evaluation.
     */
    fun cancelScheduled() {
        scheduledJob?.cancel()
        scheduledJob = null
    }

    /**
     * Cancels all activity associated with this dispatcher, including
     * the current evaluation and any scheduled evaluations.
     */
    fun shutdown() {
        workerJob?.cancel()
        workerJob = null
        scheduledJob?.cancel()
        scheduledJob = null
    }

    private fun maybeStartWorker() {
        if (workerRunning.compareAndSet(false, true)) {
            workerJob = RuntimeScope.scope.launch {
                try {
                    while (trigger.tryReceive().isSuccess) {
                        evaluate()
                    }
                } finally {
                    workerJob = null
                    workerRunning.set(false)
                    // An item may have arrived between the last tryReceive and releasing the gate.
                    // If so, re-trigger so it isn't stranded.
                    if (trigger.tryReceive().isSuccess) {
                        requestEvaluate()
                    }
                }
            }
        }
    }
}