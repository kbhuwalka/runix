package runix.primitives

import runix.core.logging.primitives.RunixExecutionContext
import runix.primitives.tracing.ExecutionTrace
import runix.primitives.tracing.child
import runix.tracing.ExecutionStatus
import runix.tracing.TraceLogEntry
import java.time.Instant
import kotlin.time.Duration
class ActionContext private constructor(
    private val base: RunixExecutionContext
) {
    val trace: ExecutionTrace get() = base.trace

    // Internal use only – not exposed to devs
    private val logger = base.scheduler.traceLogger
    private val traceManager = base.scheduler.traceManager
    private val scheduler = base.scheduler
    private val awaiter = base.awaiter

    // --- Child Execution Helpers ---

    suspend fun runChildAndWait(action: Action): ActionResult {
        val childTrace = trace.child("Run")
        return scheduler.runNowAndWait(action, childTrace)
    }

    fun scheduleChild(action: Action) {
        val childTrace = trace.child("Schedule")
        scheduler.schedule(action, childTrace)
    }

    fun fireSignal(signal: Signal) {
        val childTrace = scheduler.childTraceFor("Signal", trace)
        scheduler.fireSignal(signal, childTrace)
    }

    // --- Internal Logging Helpers ---

    internal fun logSuccess(name: String, message: String, duration: Duration) {
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

    internal fun logFailure(name: String, reason: String, recoverable: Boolean, duration: Duration) {
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
                context = mapOf(
                    "message" to reason,
                    "recoverable" to recoverable.toString()
                )
            )
        )
        traceManager.complete(trace.id, "Failure")
    }

    internal fun logSkipped(name: String, reason: String) {
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
                context = mapOf("reason" to reason)
            )
        )
        traceManager.complete(trace.id, "Skipped")
    }

    internal fun logTimeout(name: String, duration: Duration) {
        trace.logTimeout("Action [$name] timed out after $duration")
        logger?.log(
            TraceLogEntry(
                id = trace.id,
                parentId = trace.parentId,
                type = "Action",
                name = name,
                timestamp = Instant.now(),
                durationMs = duration.inWholeMilliseconds,
                status = ExecutionStatus.Timeout,
                tracePath = trace.path
            )
        )
        traceManager.complete(trace.id, "Timeout")
    }

    companion object {
        fun from(ctx: RunixExecutionContext): ActionContext = ActionContext(ctx)
    }
}