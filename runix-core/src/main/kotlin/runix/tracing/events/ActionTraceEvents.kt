package runix.tracing.events

import java.time.Instant
import java.util.UUID

/**
 * Emitted when an Action begins execution.
 */
data class ActionStarted(
    override val timestamp: Instant = Instant.now(),
    override val traceId: UUID = UUID.randomUUID(),
    override val componentPath: String,
    val actionName: String
) : TraceEvent

/**
 * Emitted when an Action completes successfully.
 */
data class ActionSucceeded(
    override val timestamp: Instant = Instant.now(),
    override val traceId: UUID,
    override val componentPath: String,
    val result: String,
    val durationMillis: Long
) : TraceEvent

/**
 * Emitted when an Action fails during execution.
 */
data class ActionFailed(
    override val timestamp: Instant = Instant.now(),
    override val traceId: UUID,
    override val componentPath: String,
    val reason: String,
    val durationMillis: Long
) : TraceEvent

/**
 * Emitted when an Action is cancelled.
 */
data class ActionCancelled(
    override val timestamp: Instant = Instant.now(),
    override val traceId: UUID,
    override val componentPath: String,
    val reason: String
) : TraceEvent

/**
 * Emitted when an Action exceeds its time limit and is forcefully terminated.
 */
data class ActionTimedOut(
    override val timestamp: Instant = Instant.now(),
    override val traceId: UUID,
    override val componentPath: String,
    val timeoutMillis: Long
) : TraceEvent

/**
 * Emitted when an Action is aborted or blocked due to a conflict with another running or queued action.
 */
data class ActionConflictDetected(
    override val timestamp: Instant = Instant.now(),
    override val traceId: UUID,
    override val componentPath: String,
    val conflictingActionName: String,
    val resolutionStrategy: String
) : TraceEvent
