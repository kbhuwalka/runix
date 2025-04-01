package runix.core.logging.primitives

import runix.primitives.Action
import runix.primitives.Reaction
import runix.primitives.RunixJob
import runix.primitives.RunixScheduler
import runix.primitives.tracing.ExecutionTrace

sealed class ExecutableCall {
    abstract fun toJob(scheduler: RunixScheduler, parentTrace: ExecutionTrace? = null): RunixJob
}

data class ActionCall<T>(
    val action: Action<T>,
    val input: T
) : ExecutableCall() {
    override fun toJob(scheduler: RunixScheduler, parentTrace: ExecutionTrace?): RunixJob {
        val trace = ExecutionTrace(
            parentId = parentTrace?.id,
            path = parentTrace?.path.orEmpty() + action.name,
            scheduler = scheduler
        )
        return RunixJob(action, trace, input)
    }
}

data class ReactionCall(
    val reaction: Reaction
) : ExecutableCall() {
    override fun toJob(scheduler: RunixScheduler, parentTrace: ExecutionTrace?): RunixJob {
        val trace = ExecutionTrace(
            parentId = parentTrace?.id,
            path = parentTrace?.path.orEmpty() + reaction.name,
            scheduler = scheduler
        )
        return RunixJob(reaction, trace)
    }
}

fun <T> Action<T>.with(input: T): ActionCall<T> = ActionCall(this, input)
fun Reaction.asCall(): ReactionCall = ReactionCall(this)