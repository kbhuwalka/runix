package runix.primitives

import runix.tracing.ExecutionStatus
import runix.tracing.ExecutionTrace
import runix.tracing.Trace
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

    suspend fun runAndWait(action: Action): ActionResult {
        val childTrace = Trace.child("Run", trace)
        return scheduler.runNowAndWait(action, childTrace)
    }

    fun schedule(action: Action) {
        val childTrace = Trace.child("Schedule", trace)
        scheduler.schedule(action, childTrace)
    }

    fun fireSignal(signal: Signal) {
        val childTrace = Trace.child("Signal", trace)
        scheduler.fireSignal(signal, childTrace)
    }

    // --- Internal Logging Helpers ---

    internal fun logSuccess(name: String, message: String, duration: Duration) {
        trace.logSuccess(message)
        Trace.log(trace, type = "Action", status = ExecutionStatus.Success, logger = logger, message)
        traceManager.complete(trace.id, message)
    }

    internal fun logFailure(name: String, reason: String, recoverable: Boolean, duration: Duration) {
        trace.logFailure(reason, recoverable)
        Trace.log(trace, type = "Action", status = ExecutionStatus.Failure, logger = logger, message = reason)
        traceManager.complete(trace.id, "Failure")
    }

    internal fun logSkipped(name: String, reason: String) {
        trace.logSkipped(reason)
        Trace.log(trace, type = "Action", status = ExecutionStatus.Skipped, logger = logger, message = reason)
        traceManager.complete(trace.id, "Skipped")
    }

    internal fun logTimeout(name: String, duration: Duration) {
        trace.logTimeout("Action [$name] timed out after $duration")
        Trace.log(trace, type = "Action", status = ExecutionStatus.Timeout, logger = logger, message = "Timed out after $duration")
        traceManager.complete(trace.id, "Timeout")
    }

    companion object {
        fun from(ctx: RunixExecutionContext): ActionContext = ActionContext(ctx)
    }
}
