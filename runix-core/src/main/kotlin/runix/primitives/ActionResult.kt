package runix.primitives

sealed interface ActionResult {
    data class Success(val message: String? = null) : ActionResult
    data class Failure(val error: ActionError) : ActionResult
    data class Cancelled(val reason: CancellationReason) : ActionResult
    object Skipped : ActionResult
    object TimedOut : ActionResult
}

enum class CancellationReason {
    SignalFired,
    ConflictResolution,
    Timeout,
    External
}
