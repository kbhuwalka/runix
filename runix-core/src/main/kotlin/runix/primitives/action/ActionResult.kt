package runix.primitives.action

/**
 * Represents the result of executing a Runix [Action].
 *
 * Actions are suspendable procedures that can produce various outcomes. This sealed class
 * provides a type-safe way to handle all possible action execution results, enabling proper
 * error handling, flow control, and operational visibility.
 *
 * Use ActionResult to:
 * - Handle different execution outcomes with pattern matching
 * - Coordinate dependent behaviors across modules
 * - Provide detailed diagnostics in error cases
 * - Support testing and verification
 * - Enable consistent telemetry and monitoring
 */
sealed class ActionResult {

    /**
     * Indicates the action was already running when a new request was made.
     *
     * This happens when:
     * - An action is configured to disallow concurrent execution
     * - A request is made while the action is already processing another request
     * - The action is configured to reject rather than queue additional requests
     */
    object AlreadyRunning : ActionResult()

    /**
     * Indicates the action completed successfully.
     *
     * This is the expected normal outcome when an action completes its work
     * without errors or interruptions.
     */
    object Success : ActionResult()

    /**
     * Indicates the action was cancelled before completion.
     *
     * This happens when:
     * - The action was explicitly cancelled
     * - The coroutine context was cancelled
     * - The action implementation threw a CancellationException
     */
    object Cancelled: ActionResult() {

    }

    /**
     * Indicates the action failed due to an uncaught exception.
     *
     * @property cause The underlying throwable that caused the failure
     */
    data class Failure(val cause: Throwable) : ActionResult()

    /**
     * Indicates the action did not complete within its allotted time.
     *
     * @property after The timeout duration that was exceeded
     */
    data class Timeout(val after: kotlin.time.Duration) : ActionResult()

    override fun toString(): String = when (this) {
        is AlreadyRunning -> "ActionResult.AlreadyRunning"
        is Success -> "ActionResult.Success"
        is Failure -> "ActionResult.Failure(${cause::class.simpleName}: ${cause.message})"
        is Cancelled -> "ActionResult.Cancelled"
        is Timeout -> "ActionResult.Timeout(after=${after.inWholeMilliseconds}ms)"
    }
}