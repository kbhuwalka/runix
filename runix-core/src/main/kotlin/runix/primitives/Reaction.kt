package runix.primitives

import runix.tracing.ExecutionStatus
import runix.tracing.ExecutionTimer
import runix.tracing.ExecutionTrace
import runix.tracing.TraceLogEntry

abstract class Reaction protected constructor(
    override val name: String,
    val signalNames: List<Signal> = emptyList()
) : RunixExecutable {

    abstract suspend fun onFired(context: ReactionContext)

    override suspend fun runWithContext(context: RunixExecutionContext) {
        val trace = context.trace
        val scheduler = context.scheduler
        val logger = scheduler.traceLogger
        val traceManager = scheduler.traceManager
        val timer = ExecutionTimer.start()

        try {
            onFired(ReactionContext.from(context))

            logger?.log(
                TraceLogEntry(
                    id = trace.id,
                    parentId = trace.parentId,
                    type = "Reaction",
                    name = name,
                    timestamp = timer.startTime,
                    durationMs = timer.elapsed().inWholeMilliseconds,
                    status = ExecutionStatus.Success,
                    tracePath = trace.path
                )
            )
            traceManager.complete(trace.id, "Success")
        } catch (e: Exception) {
            logger?.log(
                TraceLogEntry(
                    id = trace.id,
                    parentId = trace.parentId,
                    type = "Reaction",
                    name = name,
                    timestamp = timer.startTime,
                    durationMs = timer.elapsed().inWholeMilliseconds,
                    status = ExecutionStatus.Failure,
                    tracePath = trace.path,
                    context = mapOf("error" to (e.message ?: "unknown"))
                )
            )
            traceManager.complete(trace.id, "Failure")
            throw e
        }
    }

    override suspend fun execute(trace: ExecutionTrace) {
        error("Use runWithContext(...) instead.")
    }
}
