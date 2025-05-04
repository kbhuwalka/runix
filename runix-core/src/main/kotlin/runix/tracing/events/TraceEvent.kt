package runix.tracing.events

import java.time.Instant
import java.util.UUID

/**
 * Base type for all structured trace events emitted by the Runix runtime.
 * All concrete events must implement this interface.
 */
sealed interface TraceEvent {
    val timestamp: Instant
    val traceId: UUID
    val componentPath: String
}
