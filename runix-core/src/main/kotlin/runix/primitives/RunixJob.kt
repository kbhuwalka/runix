package runix.primitives

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import runix.core.RunixScheduler

data class RunixJob(
    val executable: RunixExecutable,
    val awaiter: CompletableDeferred<ActionResult>? = null,
    var job: Job? = null
) {
    suspend fun run(scheduler: RunixScheduler) {
        val context = RunixExecutionContext(
            scheduler = scheduler,
            awaiter = awaiter
        )

        try {
            executable.runWithContext(context)
        } catch (e: Exception) {
            throw e
        }
    }
}
