package runix.primitives

import runix.core.logger
import runix.core.logging.primitives.RunixExecutionContext
import runix.primitives.tracing.ExecutionTrace
import runix.primitives.tracing.child
import runix.tracing.ExecutionStatus
import runix.tracing.TraceLogEntry
import java.time.Instant

class MonitorContext private constructor(
    val trace: ExecutionTrace,
    private val base: RunixExecutionContext,
) {
    private val scheduler = base.scheduler
    private val traceManager = scheduler.traceManager

    fun emit(signal: Signal) {
        val childTrace = scheduler.childTraceFor("MonitorTrigger", trace)
        scheduler.fireSignal(signal, parentTrace = childTrace)
    }

    fun logTriggered(name: String, message: String) {
        val logger = scheduler.traceLogger
        trace.logSuccess(message)
        logger?.log(
            TraceLogEntry(
                id = trace.id,
                parentId = trace.parentId,
                type = "Monitor",
                name = name,
                timestamp = Instant.now(),
                durationMs = 0,
                status = ExecutionStatus.Triggered,
                tracePath = trace.path,
                context = mapOf("message" to message)
            )
        )
        traceManager.complete(trace.id, message)
    }

    fun logSkipped(name: String, reason: String) {
        val logger = scheduler.traceLogger
        trace.logSkipped(reason)
        logger?.log(
            TraceLogEntry(
                id = trace.id,
                parentId = trace.parentId,
                type = "Monitor",
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

    companion object {
        fun from(ctx: RunixExecutionContext, monitorName: String): MonitorContext {
            val trace = ExecutionTrace(
                path = listOf("Monitor($monitorName)"),
                scheduler = ctx.scheduler
            )
            return MonitorContext(trace, ctx)
        }
    }
}