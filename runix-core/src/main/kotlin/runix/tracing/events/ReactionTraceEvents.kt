package runix.tracing.events

import java.time.Instant
import java.util.UUID

/**
 * Emitted when a Reaction is executed due to a triggering Monitor.
 */
data class ReactionTriggered(
    override val timestamp: Instant = Instant.now(),
    override val traceId: UUID = UUID.randomUUID(),
    override val parentId: UUID?,
    val reactionName: String
) : TraceEvent