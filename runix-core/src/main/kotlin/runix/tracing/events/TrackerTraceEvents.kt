package runix.tracing.events

import java.time.Instant
import java.util.UUID

/**
 * Emitted when a Tracker’s observed value changes in a way that affects a condition.
 * This is throttled and should be emitted only when semantically meaningful.
 */
data class TrackerStateChanged(
    override val timestamp: Instant = Instant.now(),
    override val traceId: UUID = UUID.randomUUID(),
    override val componentPath: String,
    val trackerKey: String,
    val newValue: Any?,
    val causedEvaluation: Boolean
) : TraceEvent