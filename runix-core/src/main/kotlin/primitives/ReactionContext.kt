package runix.primitives

import runix.core.logging.primitives.RunixExecutionContext
import runix.primitives.tracing.ExecutionTrace
import runix.primitives.tracing.child

class ReactionContext private constructor(
    private val base: RunixExecutionContext
) {
    val trace: ExecutionTrace get() = base.trace

    // Internal only
    private val scheduler = base.scheduler

    suspend fun runChildAndWait(action: Action): ActionResult {
        val childTrace = trace.child("Run")
        return scheduler.runNowAndWait(action, childTrace)
    }

    fun scheduleChild(action: Action) {
        val childTrace = trace.child("Schedule")
        scheduler.schedule(action, childTrace)
    }

    fun fireSignal(signal: Signal) {
        scheduler.fireSignal(signal)
    }

    fun runAll(vararg actions: Action) {
        actions.forEach { scheduleChild(it) }
    }

    companion object {
        fun from(ctx: RunixExecutionContext): ReactionContext = ReactionContext(ctx)
    }
}