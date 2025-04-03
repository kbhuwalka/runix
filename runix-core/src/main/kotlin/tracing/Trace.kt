package runix.tracing

import runix.primitives.RunixScheduler
import runix.primitives.tracing.ExecutionTrace
import java.time.Instant

object Trace {
    fun child(label: String, parent: ExecutionTrace): ExecutionTrace {
        return ExecutionTrace(
            parentId = parent.id,
            path = parent.path + label,
            scheduler = parent.scheduler
        )
    }

    fun root(name: String, scheduler: RunixScheduler): ExecutionTrace {
        return ExecutionTrace(
            parentId = null,
            path = listOf(name),
            scheduler = scheduler
        )
    }

    fun log(
        trace: ExecutionTrace,
        type: String,
        status: ExecutionStatus,
        logger: TraceLogger?,
        message: String = type
    ) {
        logger?.log(
            TraceLogEntry(
                id = trace.id,
                parentId = trace.parentId,
                type = type,
                name = trace.path.lastOrNull() ?: "Unknown",
                timestamp = Instant.now(),
                durationMs = 0,
                status = status,
                tracePath = trace.path,
                context = mapOf("message" to message)
            )
        )
    }
}