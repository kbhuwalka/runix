package runix.primitives

import runix.core.logging.primitives.RunixExecutionContext
import runix.primitives.tracing.ExecutionTrace
import runix.tracing.ExecutionStatus
import runix.tracing.TraceLogEntry
import java.time.Instant
import kotlin.time.Duration

class ActionContext private constructor(
    private val base: RunixExecutionContext
) {
    val trace: ExecutionTrace get() = base.trace
    val scheduler: RunixScheduler get() = base.scheduler
    val awaiter = base.awaiter
    val timeout: Duration = base.timeout

    private val logger = scheduler.traceLogger
    private val traceManager = scheduler.traceManager

    fun logSuccess(name: String, message: String, duration: Duration) {
        trace.logSuccess(message)
        logger?.log(
            TraceLogEntry(
                id = trace.id,
                parentId = trace.parentId,
                type = "Action",
                name = name,
                timestamp = Instant.now(),
                durationMs = duration.inWholeMilliseconds,
                status = ExecutionStatus.Success,
                tracePath = trace.path,
                context = mapOf("message" to message)
            )
        )
        traceManager.complete(trace.id, message)
    }

    fun logFailure(name: String, reason: String, recoverable: Boolean, duration: Duration) {
        trace.logFailure(reason, recoverable)
        logger?.log(
            TraceLogEntry(
                id = trace.id,
                parentId = trace.parentId,
                type = "Action",
                name = name,
                timestamp = Instant.now(),
                durationMs = duration.inWholeMilliseconds,
                status = ExecutionStatus.Failure,
                tracePath = trace.path,
                context = mapOf("message" to reason, "recoverable" to recoverable.toString())
            )
        )
        traceManager.complete(trace.id, "Failure")
    }

    fun logSkipped(name: String, reason: String) {
        trace.logSkipped(reason)
        logger?.log(
            TraceLogEntry(
                id = trace.id,
                parentId = trace.parentId,
                type = "Action",
                name = name,
                timestamp = Instant.now(),
                durationMs = 0,
                status = ExecutionStatus.Skipped,
                tracePath = trace.path,
                context = mapOf("message" to reason)
            )
        )
        traceManager.complete(trace.id, "Skipped")
    }

    fun logTimeout(name: String) {
        trace.logTimeout("Action [$name] timed out after $timeout")
        logger?.log(
            TraceLogEntry(
                id = trace.id,
                parentId = trace.parentId,
                type = "Action",
                name = name,
                timestamp = Instant.now(),
                durationMs = timeout.inWholeMilliseconds,
                status = ExecutionStatus.Timeout,
                tracePath = trace.path
            )
        )
        traceManager.complete(trace.id, "Timeout")
    }

    companion object {
        fun from(ctx: RunixExecutionContext) = ActionContext(ctx)
    }
}