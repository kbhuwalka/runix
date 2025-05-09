package runix.tracing.events

import java.time.Instant
import java.util.UUID

/**
 * Emitted when an Action begins execution.
 */
data class ActionStarted(
    override val timestamp: Instant = Instant.now(),
    override val traceId: UUID = UUID.randomUUID(),
    override val parentId: UUID?,
    val actionName: String
) : TraceEvent

/**
 * Emitted when an Action completes successfully.
 */
data class ActionSucceeded(
    override val timestamp: Instant = Instant.now(),
    override val traceId: UUID,
    override val parentId: UUID?,
    val result: String,
    val durationMillis: Long
) : TraceEvent

/**
 * Emitted when an Action fails during execution.
 */
data class ActionFailed(
    override val timestamp: Instant = Instant.now(),
    override val traceId: UUID,
    override val parentId: UUID?,
    val reason: String,
    val durationMillis: Long
) : TraceEvent

/**
 * Emitted when an Action is cancelled.
 */
data class ActionCancelled(
    override val timestamp: Instant = Instant.now(),
    override val traceId: UUID,
    override val parentId: UUID?,
    val reason: String
) : TraceEvent

/**
 * Emitted when an Action exceeds its time limit and is forcefully terminated.
 */
data class ActionTimedOut(
    override val timestamp: Instant = Instant.now(),
    override val traceId: UUID,
    override val parentId: UUID?,
    val timeoutMillis: Long
) : TraceEvent
