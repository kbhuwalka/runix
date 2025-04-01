package runix.primitives

import kotlinx.coroutines.flow.StateFlow
import runix.core.logging.primitives.ActionCall
import runix.core.logging.primitives.RunixExecutionContext
import runix.primitives.tracing.ExecutionTrace
import runix.primitives.tracing.child
import runix.tracing.ExecutionStatus
import runix.tracing.ExecutionTimer
import runix.tracing.TraceLogEntry

class ReactionContext(private val base: RunixExecutionContext) {
    val trace: ExecutionTrace get() = base.trace
    val scheduler: RunixScheduler get() = base.scheduler

    companion object {
        fun from(ctx: RunixExecutionContext): ReactionContext = ReactionContext(ctx)
    }

    suspend fun <T> run(call: ActionCall<T>) {
        val childTrace = trace.child(call.action.name)
        scheduler.schedule(call.action, call.input, childTrace)
    }

    suspend fun runAll(vararg calls: ActionCall<*>) {
        calls.forEach { call ->
            @Suppress("UNCHECKED_CAST")
            run(call as ActionCall<Any?>)
        }
    }
}

class Reaction(
    override val name: String,
    val dependsOn: List<IRunixFlow<*>>,
    val signalNames: List<String> = emptyList(),
    val condition: () -> Boolean,
    val onFired: suspend (ReactionContext) -> Unit
) : RunixExecutable {

    override suspend fun runWithContext(context: RunixExecutionContext) {
        val trace = context.trace
        val logger = context.scheduler.traceLogger
        val traceManager = context.scheduler.traceManager
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
            traceManager.complete(trace.id, "Failed")
            throw e
        }
    }

    override suspend fun execute(trace: ExecutionTrace) {
        error("Use runWithContext(...) instead of execute()")
    }
}