package runix.primitives

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import runix.core.logging.primitives.RunixExecutionContext
import runix.primitives.tracing.ExecutionTrace
import runix.tracing.ExecutionTimer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

sealed interface ActionResult {
    data class Success(val message: String? = null) : ActionResult
    data class Failure(val message: String, val recoverable: Boolean = true) : ActionResult
}

abstract class Action(
    override val name: String
) : RunixExecutable {

    open val timeout: Duration = 5.seconds

    open fun onPreconditions(): Boolean = true

    open fun onSuccess(result: ActionResult.Success, trace: ExecutionTrace) {}
    open fun onFailure(result: ActionResult.Failure, trace: ExecutionTrace) {}
    open fun onTimeout(trace: ExecutionTrace) {}
    open fun onSkipped(trace: ExecutionTrace) {}

    abstract suspend fun onExecute(context: ActionContext): ActionResult

    override suspend fun runWithContext(context: RunixExecutionContext) {
        val trace = context.trace
        val actionContext = ActionContext.from(context)
        val timer = ExecutionTimer.start()

        if (!onPreconditions()) {
            actionContext.logSkipped(name, "Preconditions not met")
            onSkipped(trace)
            return
        }

        try {
            val result = withTimeout(timeout.inWholeMilliseconds) {
                onExecute(actionContext)
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
            val fail = ActionResult.Failure("Timed out", recoverable = true)
            actionContext.logTimeout(name, timeout)
            context.awaiter?.complete(fail)
            onTimeout(trace)

        } catch (e: Exception) {
            val reason = e.message ?: "Unknown error"
            val fail = ActionResult.Failure(reason, recoverable = false)
            actionContext.logFailure(name, reason, recoverable = false, timer.elapsed())
            context.awaiter?.complete(fail)
            onFailure(fail, trace)
        }
    }

    override suspend fun execute(trace: ExecutionTrace) {
        error("Use runWithContext(...) instead.")
    }
}