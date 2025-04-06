package runix.primitives

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import runix.core.RunixScheduler
import runix.tracing.ExecutionStatus
import runix.tracing.ExecutionTrace
import runix.tracing.Trace
import java.time.Instant

data class RunixJob(
    val executable: RunixExecutable,
    val trace: ExecutionTrace,
    val awaiter: CompletableDeferred<ActionResult>? = null,
    var job: Job? = null
) {
    suspend fun run(scheduler: RunixScheduler) {
        val context = RunixExecutionContext(
            trace = trace,
            scheduler = scheduler,
            awaiter = awaiter
        )
        val logger = scheduler.traceLogger
        Trace.log(trace, "Job", ExecutionStatus.Started, logger, startTime = trace.startTime)

        try {
            executable.runWithContext(context)
            Trace.log(trace, "Job", ExecutionStatus.Success, logger, startTime = trace.startTime, endTime = Instant.now())
        } catch (e: Exception) {
            Trace.log(trace, "Job", ExecutionStatus.Failure, logger, message = "Error: ${e.message}", exception = e.toString(), startTime = trace.startTime, endTime = Instant.now())
            throw e
        }
    }
}
