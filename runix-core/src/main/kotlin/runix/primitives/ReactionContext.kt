package runix.primitives

class ReactionContext private constructor(
    private val base: RunixExecutionContext
) {

    // Internal only
    private val scheduler = base.scheduler

    suspend fun runAndWait(action: Action): ActionResult {
        return scheduler.runAndWait(action)
    }

    fun schedule(action: Action) {
        scheduler.run(action)
    }

    fun fire(signal: Signal) {
        scheduler.fireSignal(signal)
    }

    fun runAll(vararg actions: Action) {
        actions.forEach { schedule(it) }
    }

    companion object {
        fun from(ctx: RunixExecutionContext): ReactionContext = ReactionContext(ctx)
    }
}
