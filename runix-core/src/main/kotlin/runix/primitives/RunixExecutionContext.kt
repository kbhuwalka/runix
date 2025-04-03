package runix.primitives

import kotlinx.coroutines.CompletableDeferred
import runix.core.RunixScheduler
import runix.tracing.ExecutionTrace
import runix.tracing.Trace

/**
 * Unified context used during execution of any RunixExecutable.
 */
data class RunixExecutionContext(
    val trace: ExecutionTrace,
    val scheduler: RunixScheduler,
    val awaiter: CompletableDeferred<ActionResult>? = null
) {
    suspend fun RunixExecutionContext.runAndWait(action: Action): ActionResult {
        val childTrace = Trace.child(action.name, trace)
        return scheduler.runNowAndWait(action, childTrace)
    }

    fun RunixExecutionContext.schedule(action: Action) {
        val childTrace = Trace.child(action.name, trace)
        scheduler.schedule(action, childTrace)
    }

    fun RunixExecutionContext.runAll(vararg actions: Action) {
        actions.forEach { schedule(it) }
    }
}
