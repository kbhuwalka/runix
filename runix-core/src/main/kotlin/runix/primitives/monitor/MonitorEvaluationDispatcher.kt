package runix.primitives.monitor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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
 */
class MonitorEvaluationDispatcher(
    private val evaluate: suspend () -> Unit
) {

    private var activeJob: Job? = null
    private var scheduledJob: Job? = null
    private val pending = AtomicBoolean(false)

    /**
     * Requests an evaluation to occur as soon as possible.
     * If one is already running, it will coalesce to run once more after.
     */
    fun requestEvaluate() {
        if (activeJob == null) {
            runEvaluation()
        } else {
            pending.set(true)
        }
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
        activeJob?.cancel()
        scheduledJob?.cancel()
        pending.set(false)
    }

    private fun runEvaluation() {
        activeJob = RuntimeScope.scope.launch {
            try {
                evaluate()
            } finally {
                activeJob = null
                if (pending.getAndSet(false)) {
                    runEvaluation()
                }
            }
        }
    }
}