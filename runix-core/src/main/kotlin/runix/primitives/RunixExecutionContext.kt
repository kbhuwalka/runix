package runix.primitives

import kotlinx.coroutines.CompletableDeferred
import runix.core.RunixScheduler

/**
 * Unified context used during execution of any RunixExecutable.
 */
data class RunixExecutionContext(
    val scheduler: RunixScheduler,
    val awaiter: CompletableDeferred<ActionResult>? = null,
    val actor: String? = null,
    val tags: List<String> = emptyList()
) {
    suspend fun runAndWait(action: Action): ActionResult {
        return scheduler.runAndWait(action)
    }

    fun schedule(action: Action) {
        scheduler.run(action)
    }

    fun runAll(vararg actions: Action) {
        actions.forEach { schedule(it) }
    }

    fun fire(signal: Signal) {
        scheduler.fireSignal(signal)
    }
}
