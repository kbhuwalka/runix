package runix.core.logging.primitives

import kotlinx.coroutines.CompletableDeferred
import runix.primitives.ActionResult
import runix.primitives.RunixScheduler
import runix.primitives.tracing.ExecutionTrace
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Shared context for any RunixExecutable.
 */
data class RunixExecutionContext(
    val trace: ExecutionTrace,
    val scheduler: RunixScheduler,
    val input: Any? = null,
    val awaiter: CompletableDeferred<ActionResult>? = null,
    val timeout: Duration = 5.seconds
)