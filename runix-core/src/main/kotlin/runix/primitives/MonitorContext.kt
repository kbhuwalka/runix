package runix.primitives

import runix.tracing.ExecutionStatus
import runix.tracing.ExecutionTrace
import runix.tracing.Trace

class MonitorContext private constructor(
    val trace: ExecutionTrace,
    private val base: RunixExecutionContext
) {
    private val scheduler = base.scheduler
    private val traceManager = scheduler.traceManager

    fun emit(signal: Signal) {
        val childTrace = Trace.child(
            label = "MonitorTrigger",
            parent = trace,
            actor = trace.actor,
            tags = trace.tags
        )
        scheduler.fireSignal(signal, parentTrace = childTrace)
    }

    fun logTriggered(name: String, message: String) {
        trace.logSuccess(message)
        Trace.log(
            trace,
            type = "Monitor",
            status = ExecutionStatus.Triggered,
            logger = scheduler.traceLogger,
            message = message,
            actor = trace.actor,
            tags = trace.tags,
            startTime = trace.startTime
        )
        traceManager.complete(trace.id, message)
    }

    fun logSkipped(name: String, reason: String) {
        trace.logSkipped(reason)
        Trace.log(
            trace,
            type = "Monitor",
            status = ExecutionStatus.Skipped,
            logger = scheduler.traceLogger,
            message = reason,
            actor = trace.actor,
            tags = trace.tags,
            startTime = trace.startTime
        )
        traceManager.complete(trace.id, "Skipped")
    }

    companion object {
        fun from(ctx: RunixExecutionContext, monitorName: String): MonitorContext {
            val trace = Trace.root("Monitor($monitorName)", ctx.scheduler)
            return MonitorContext(trace, ctx)
        }
    }
}
