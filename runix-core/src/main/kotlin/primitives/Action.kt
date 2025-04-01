package runix.primitives

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import runix.core.logging.primitives.RunixExecutionContext
import runix.primitives.tracing.ExecutionTrace
import runix.tracing.ExecutionTimer

sealed interface ActionResult {
    data class Success(val message: String? = null) : ActionResult
    data class Failure(val message: String, val recoverable: Boolean = true) : ActionResult
}

abstract class Action<T>(override val name: String) : RunixExecutable {

    /** Core logic for this action */
    abstract suspend fun onExecute(params: T, context: ActionContext): ActionResult

    /** Optional precondition check */
    open fun onPreconditions(params: T): Boolean = true

    /** Called when action succeeds */
    open fun onSuccess(result: ActionResult.Success, trace: ExecutionTrace) {}

    /** Called when action fails */
    open fun onFailure(result: ActionResult.Failure, trace: ExecutionTrace) {}

    /** Called when action times out */
    open fun onTimeout(trace: ExecutionTrace) {}

    /** Called when preconditions are not met */
    open fun onSkipped(trace: ExecutionTrace) {}

    override suspend fun runWithContext(context: RunixExecutionContext) {
        val input = context.input as? T ?: error("Missing input for Action: $name")
        val actionContext = ActionContext.from(context)
        val trace = context.trace

        val timer = ExecutionTimer.start()

        if (!onPreconditions(input)) {
            actionContext.logSkipped(name, "Preconditions not met")
            onSkipped(trace)
            return
        }

        try {
            val result = withTimeout(actionContext.timeout.inWholeMilliseconds) {
                onExecute(input, actionContext)
            }

            when (result) {
                is ActionResult.Success -> {
                    actionContext.logSuccess(name, result.message ?: "Success", timer.elapsed())
                    context.awaiter?.complete(result)
                    onSuccess(result, trace)
                }

                is ActionResult.Failure -> {
                    actionContext.logFailure(name, result.message, result.recoverable, timer.elapsed())
                    context.awaiter?.complete(result)
                    onFailure(result, trace)
                }
            }

        } catch (e: TimeoutCancellationException) {
            actionContext.logTimeout(name)
            context.awaiter?.complete(ActionResult.Failure("Timeout"))
            onTimeout(trace)
        } catch (e: Exception) {
            val reason = "Unhandled exception: ${e.message ?: "unknown"}"
            actionContext.logFailure(name, reason, recoverable = false, duration = timer.elapsed())
            context.awaiter?.complete(ActionResult.Failure(reason, recoverable = false))
            onFailure(ActionResult.Failure(reason, recoverable = false), trace)
        }
    }

    /** Prevent incorrect execution */
    override suspend fun execute(trace: ExecutionTrace) {
        error("Use run(...) with parameters and context instead of execute()")
    }
}