package runix.primitives

import java.time.Instant

abstract class Reaction protected constructor(
    override val name: String,
    val signalNames: List<Signal> = emptyList()
) : RunixExecutable {

    abstract suspend fun onFired(context: ReactionContext)

    override suspend fun runWithContext(context: RunixExecutionContext) {
        val scheduler = context.scheduler

        try {
            onFired(ReactionContext.from(context))
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun execute() {
        error("Use runWithContext(...) instead.")
    }
}
