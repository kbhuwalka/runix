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
    val awaiter: CompletableDeferred<ActionResult>? = null,
    val actor: String? = null,
    val tags: List<String> = emptyList()
) {
    suspend fun runAndWait(action: Action): ActionResult {
        val childTrace = Trace.child(action.name, trace, actor = action.actor, tags = action.tags)
        return scheduler.runNowAndWait(action, childTrace)
    }

    fun schedule(action: Action) {
        val childTrace = Trace.child(action.name, trace, actor = action.actor, tags = action.tags)
        scheduler.schedule(action, childTrace)
    }

    fun runAll(vararg actions: Action) {
        actions.forEach { schedule(it) }
    }

    fun fire(signal: Signal) {
        val childTrace = Trace.child(signal.name, trace)
        scheduler.fireSignal(signal, childTrace)
    }
}
