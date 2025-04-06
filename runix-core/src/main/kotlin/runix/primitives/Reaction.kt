package runix.primitives

import runix.tracing.ExecutionStatus
import runix.tracing.ExecutionTrace
import runix.tracing.Trace
import java.time.Instant

abstract class Reaction protected constructor(
    override val name: String,
    val signalNames: List<Signal> = emptyList()
) : RunixExecutable {

    abstract suspend fun onFired(context: ReactionContext)

    override suspend fun runWithContext(context: RunixExecutionContext) {
        val trace = context.trace
        val scheduler = context.scheduler
        val logger = scheduler.traceLogger

        try {
            Trace.log(
                trace = trace,
                type = "Reaction",
                status = ExecutionStatus.Started,
                logger = logger,
                actor = trace.actor,
                tags = trace.tags,
                startTime = trace.startTime
            )

            onFired(ReactionContext.from(context))

            Trace.log(
                trace = trace,
                type = "Reaction",
                status = ExecutionStatus.Success,
                logger = logger,
                actor = trace.actor,
                tags = trace.tags,
                startTime = trace.startTime,
                endTime = Instant.now()
            )
            context.scheduler.traceManager.complete(trace.id, "Success")
        } catch (e: Exception) {
            Trace.log(
                trace = trace,
                type = "Reaction",
                status = ExecutionStatus.Failure,
                logger = logger,
                actor = trace.actor,
                tags = trace.tags,
                startTime = trace.startTime,
                endTime = Instant.now(),
                exception = e.toString()
            )
            context.scheduler.traceManager.complete(trace.id, "Failure")
            throw e
        }
    }

    override suspend fun execute(trace: ExecutionTrace) {
        error("Use runWithContext(...) instead.")
    }
}
