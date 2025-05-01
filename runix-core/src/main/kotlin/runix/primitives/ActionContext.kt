package runix.primitives

class ActionContext private constructor(
    private val base: RunixExecutionContext
) {

    // Internal use only – not exposed to devs
    private val scheduler = base.scheduler
    private val awaiter = base.awaiter

    // --- Child Execution Helpers ---

    suspend fun runAndWait(action: Action): ActionResult {
        return scheduler.runAndWait(action)
    }

    fun schedule(action: Action) {
        scheduler.run(action)
    }

    fun fireSignal(signal: Signal) {
        scheduler.fireSignal(signal)
    }

    companion object {
        fun from(ctx: RunixExecutionContext): ActionContext = ActionContext(ctx)
    }
}
