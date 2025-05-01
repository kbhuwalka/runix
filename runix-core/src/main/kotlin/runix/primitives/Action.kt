package runix.primitives

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

abstract class Action(
    override val name: String
) : RunixExecutable {

    open val timeout: Duration = 5.seconds
    open val conflictPolicy: ConflictPolicy = ConflictPolicy.Allow
    open val cancelOn: List<Signal> = emptyList()
    open val actor: String? = this::class.simpleName
    open val tags: List<String> = emptyList()

    abstract suspend fun onExecute(context: ActionContext): ActionResult

    open suspend fun onComplete(result: ActionResult) {}

    override suspend fun runWithContext(context: RunixExecutionContext) {
        val actionContext = ActionContext.from(context)

        val result = try {
            withTimeout(timeout.inWholeMilliseconds) {
                onExecute(actionContext)
            }
        } catch (e: TimeoutCancellationException) {
            ActionResult.Cancelled(CancellationReason.Timeout)
        } catch (e: CancellationException) {
            ActionResult.Cancelled(CancellationReason.SignalFired)
        } catch (e: Exception) {
            ActionResult.Failure(ActionError(code = "exception", message = e.message ?: "Unknown error"))
        }

        onComplete(result)
        context.awaiter?.complete(result)
    }

    override suspend fun execute() {
        error("Use runWithContext(...) instead.")
    }
}
