package runix.tracing.events

import java.time.Instant
import java.util.UUID

/**
 * Emitted when a Reaction is executed due to a triggering Monitor.
 */
data class ReactionTriggered(
    override val timestamp: Instant = Instant.now(),
    override val traceId: UUID = UUID.randomUUID(),
    override val componentPath: String,
    val reactionName: String
) : TraceEvent

/**
 * Emitted when a Reaction was skipped because its Monitor was not active.
 */
data class ReactionSkipped(
    override val timestamp: Instant = Instant.now(),
    override val traceId: UUID = UUID.randomUUID(),
    override val componentPath: String,
    val reason: String
) : TraceEvent