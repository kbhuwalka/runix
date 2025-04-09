package runix.primitives

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import runix.tracing.ExecutionTrace
import java.time.Instant
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

    open suspend fun onComplete(result: ActionResult, trace: ExecutionTrace) {}

    override suspend fun runWithContext(context: RunixExecutionContext) {
        val trace = context.trace
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

        when (result) {
            is ActionResult.Success -> {
                actionContext.logSuccess(name, result.message ?: "Success", actor = this.actor, tags = this.tags, startTime = trace.startTime, endTime = Instant.now())
            }
            is ActionResult.Failure -> {
                actionContext.logFailure(name, result.error.message, actor = this.actor, tags = this.tags, startTime = trace.startTime, endTime = Instant.now())
            }
            is ActionResult.Cancelled -> {
                actionContext.logCancelled(name, actor = this.actor, tags = this.tags, startTime = trace.startTime, endTime = Instant.now())
            }
            is ActionResult.Skipped -> {
                actionContext.logSkipped(name, "Marked skipped by preconditions or scheduler", actor = this.actor, tags = this.tags, startTime = trace.startTime, endTime = Instant.now())
            }
            is ActionResult.TimedOut -> {
                // No-op
            }
        }

        onComplete(result, trace)
        context.awaiter?.complete(result)
    }

    override suspend fun execute(trace: ExecutionTrace) {
        error("Use runWithContext(...) instead.")
    }
}
