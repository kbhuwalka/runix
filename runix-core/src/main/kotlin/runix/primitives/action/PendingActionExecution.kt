package runix.primitives.action

import kotlinx.coroutines.CompletableDeferred
import runix.tracing.events.TraceEvent

/**
 * Internal representation of a pending action execution request.
 *
 * This class holds both the input data for an action and a deferred result
 * that will be completed when the action finishes execution.
 *
 * @param T the type of input data for the action
 */
internal data class PendingActionExecution<T>(
    /**
     * The input data that will be passed to the action execution.
     */
    val data: T,
    /**
     * A deferred result that will be completed with the action's execution result.
     * This allows async tracking of the action's completion status.
     */
    val deferred: CompletableDeferred<ActionResult>,
    /**
     * The trace previousEvent at the time of submission.
     * This allows proper tracing across asynchronous boundaries.
     */
    val previousEvent: TraceEvent?
    )
