package runix.primitives

import runix.tracing.ExecutionStatus
import runix.tracing.ExecutionTrace
import runix.tracing.Trace
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

    suspend fun runAndWait(action: Action): ActionResult {
        val childTrace = Trace.child("Run", trace, actor = action.actor, tags = action.tags)
        return scheduler.runAndWait(action, childTrace)
    }

    fun schedule(action: Action) {
        val childTrace = Trace.child("Schedule", trace, actor = action.actor, tags = action.tags)
        scheduler.run(action, childTrace)
    }

    fun fireSignal(signal: Signal) {
        val childTrace = Trace.child("Signal", trace, actor = signal.actor, tags = signal.tags)
        scheduler.fireSignal(signal, childTrace)
    }

    // --- Internal Logging Helpers ---

    internal fun logSuccess(name: String, message: String, actor: String? = null, tags: List<String> = emptyList(), startTime: Instant = trace.startTime, endTime: Instant = Instant.now()) {
        trace.logSuccess(message)
        val label = "${trace.path.lastOrNull() ?: "Action"}[$name]"
        Trace.log(trace, type = label, status = ExecutionStatus.Success, logger = logger, message = message, actor = actor, tags = tags, startTime = startTime, endTime = endTime)
        traceManager.complete(trace.id, label)
    }

    internal fun logFailure(name: String, reason: String, actor: String? = null, tags: List<String> = emptyList(), startTime: Instant = trace.startTime, endTime: Instant = Instant.now(), exception: String? = null) {
        trace.logFailure(reason)
        val label = "${trace.path.lastOrNull() ?: "Action"}[$name]"
        Trace.log(trace, type = label, status = ExecutionStatus.Failure, logger = logger, message = reason, actor = actor, tags = tags, startTime = startTime, endTime = endTime, exception = exception)
        traceManager.complete(trace.id, label)
    }

    internal fun logSkipped(
        name: String,
        reason: String,
        actor: String? = null,
        tags: List<String> = emptyList(),
        startTime: Instant = trace.startTime,
        endTime: Instant = Instant.now()
    ) {
        trace.logSkipped(reason)
        val label = "${trace.path.lastOrNull() ?: "Action"}[$name]"
        Trace.log(
            trace,
            type = label,
            status = ExecutionStatus.Skipped,
            logger = logger,
            message = reason,
            actor = actor,
            tags = tags,
            startTime = startTime,
            endTime = endTime
        )
        traceManager.complete(trace.id, label)
    }

    internal fun logTimeout(duration: Duration) {
        trace.logTimeout("Action timed out after $duration")
        Trace.log(trace, type = "Action", status = ExecutionStatus.Timeout, logger = logger, message = "Timed out after $duration", actor = trace.actor, tags = trace.tags, startTime = trace.startTime, endTime = Instant.now())
        traceManager.complete(trace.id, "Timeout")
    }

    internal fun logCancelled(name: String, actor: String? = null, tags: List<String> = emptyList(), startTime: Instant = trace.startTime, endTime: Instant = Instant.now()) {
        trace.logCancellation("Cancelled")
        val label = "${trace.path.lastOrNull() ?: "Action"}[$name]"
        Trace.log(trace, type = label, status = ExecutionStatus.Cancelled, logger = logger, message = "Cancelled", actor = actor, tags = tags, startTime = startTime, endTime = endTime)
        traceManager.complete(trace.id, label)
    }

    companion object {
        fun from(ctx: RunixExecutionContext): ActionContext = ActionContext(ctx)
    }
}
