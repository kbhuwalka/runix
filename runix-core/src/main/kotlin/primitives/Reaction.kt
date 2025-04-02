package runix.primitives

import runix.core.logging.primitives.RunixExecutionContext
import runix.primitives.tracing.ExecutionTrace
import runix.tracing.ExecutionStatus
import runix.tracing.ExecutionTimer
import runix.tracing.TraceLogEntry

class Reaction(
    override val name: String,
    val dependsOn: List<IRunixFlow<*>>,
    val signalNames: List<Signal> = emptyList(),
    val condition: () -> Boolean,
    val onFired: suspend (ReactionContext) -> Unit
) : RunixExecutable {

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