package runix.primitives

import runix.core.logging.primitives.RunixExecutionContext
import runix.primitives.tracing.ExecutionTrace
import runix.tracing.Trace

class ReactionContext private constructor(
    private val base: RunixExecutionContext
) {
    val trace: ExecutionTrace get() = base.trace

    // Internal only
    private val scheduler = base.scheduler

    suspend fun runAndWait(action: Action): ActionResult {
        val childTrace = Trace.child("Run", trace)
        return scheduler.runNowAndWait(action, childTrace)
    }

    fun schedule(action: Action) {
        val childTrace =Trace.child("Schedule", trace)
        scheduler.schedule(action, childTrace)
    }

    fun fireSignal(signal: Signal) {
        scheduler.fireSignal(signal)
    }

    fun runAll(vararg actions: Action) {
        actions.forEach { schedule(it) }
    }

    companion object {
        fun from(ctx: RunixExecutionContext): ReactionContext = ReactionContext(ctx)
    }
}