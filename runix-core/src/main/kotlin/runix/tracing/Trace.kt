package runix.tracing

import runix.core.RunixScheduler
import java.time.Instant

object Trace {
    fun child(
        label: String,
        parent: ExecutionTrace,
        actor: String? = null,
        tags: List<String> = emptyList(),
        causeTraceId: Long? = null
    ): ExecutionTrace {
        return ExecutionTrace(
            parentId = parent.id,
            path = parent.path + label,
            scheduler = parent.scheduler,
            actor = actor,
            tags = tags,
            causeTraceId = causeTraceId
        )
    }

    fun root(
        name: String,
        scheduler: RunixScheduler,
        actor: String? = null,
        tags: List<String> = emptyList()
    ): ExecutionTrace {
        return ExecutionTrace(
            parentId = null,
            path = listOf(name),
            scheduler = scheduler,
            actor = actor,
            tags = tags
        )
    }

    fun log(
        trace: ExecutionTrace,
        type: String,
        status: ExecutionStatus,
        logger: TraceLogger?,
        message: String = type,
        startTime: Instant? = null,
        endTime: Instant? = null,
        exception: String? = null,
        actor: String? = null,
        tags: List<String> = emptyList(),
        cause: ExecutionTrace? = null,
        context: Map<String, String> = emptyMap()
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
                startTime = startTime,
                endTime = endTime,
                exception = exception,
                actor = actor,
                tags = tags,
                causeTrace = cause?.let {
                    TraceLogEntry.CauseTrace(traceId = it.id, reason = message)
                },
                tracePath = trace.path,
                context = context
            )
        )
    }
}
