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
        val childTrace = Trace.child("MonitorTrigger", trace)
        scheduler.fireSignal(signal, parentTrace = childTrace)
    }

    fun logTriggered(name: String, message: String) {
        trace.logSuccess(message)
        Trace.log(trace, type = "Monitor", status = ExecutionStatus.Triggered, logger = scheduler.traceLogger, message)
        traceManager.complete(trace.id, message)
    }

    fun logSkipped(name: String, reason: String) {
        trace.logSkipped(reason)
        Trace.log(trace, type = "Monitor", status = ExecutionStatus.Triggered, logger = scheduler.traceLogger, reason)
        traceManager.complete(trace.id, "Skipped")
    }

    companion object {
        fun from(ctx: RunixExecutionContext, monitorName: String): MonitorContext {
            val trace = Trace.root("Monitor($monitorName)", ctx.scheduler)
            return MonitorContext(trace, ctx)
        }
    }
}
