package runix.tracing.events

import java.time.Instant
import java.util.UUID

/**
 * Emitted when a Reaction is executed due to a triggering Monitor.
 */
internal data class ReactionTriggered(
    override val timestamp: Instant = Instant.now(),
    override val traceId: UUID = UUID.randomUUID(),
    override val parent: TraceEvent?,
    val reactionName: String
) : TraceEvent