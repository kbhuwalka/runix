package runix.primitives

import kotlinx.coroutines.CompletableDeferred
import runix.core.logging.primitives.RunixExecutionContext
import runix.primitives.tracing.ExecutionTrace

data class RunixJob(
    val executable: RunixExecutable,
    val trace: ExecutionTrace,
    val input: Any? = null,
    val awaiter: CompletableDeferred<ActionResult>? = null
) {
    suspend fun run(scheduler: RunixScheduler) {
        val context = RunixExecutionContext(
            trace = trace,
            scheduler = scheduler,
            input = input,
            awaiter = awaiter
        )
        executable.runWithContext(context)
    }
}