package runix.core.logging.primitives

import kotlinx.coroutines.CompletableDeferred
import runix.primitives.Action
import runix.primitives.ActionResult
import runix.primitives.RunixScheduler
import runix.primitives.tracing.ExecutionTrace
import runix.primitives.tracing.child

/**
 * Unified context used during execution of any RunixExecutable.
 */
data class RunixExecutionContext(
    val trace: ExecutionTrace,
    val scheduler: RunixScheduler,
    val awaiter: CompletableDeferred<ActionResult>? = null
) {
    suspend fun RunixExecutionContext.runChildAndWait(action: Action): ActionResult {
        val childTrace = trace.child(action.name)
        return scheduler.runNowAndWait(action, childTrace)
    }

    fun RunixExecutionContext.scheduleChild(action: Action) {
        val childTrace = trace.child(action.name)
        scheduler.schedule(action, childTrace)
    }

    fun RunixExecutionContext.runAll(vararg actions: Action) {
        actions.forEach { scheduleChild(it) }
    }
}