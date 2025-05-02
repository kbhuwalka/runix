package runix.tracing.events

import java.time.Instant
import java.util.UUID

/**
 * Emitted when a Monitor's compositional condition becomes true and fires.
 * This is the key moment of evaluation → action transition.
 */
data class MonitorTriggered(
    override val timestamp: Instant = Instant.now(),
    override val traceId: UUID = UUID.randomUUID(),
    override val componentPath: String,
    val monitorName: String,
    val satisfiedConditions: List<String>
) : TraceEvent

/**
 * Emitted when a Monitor emits a signal due to its triggering condition.
 * This is always immediately preceded by a MonitorTriggered event.
 */
data class SignalEmitted(
    override val timestamp: Instant = Instant.now(),
    override val traceId: UUID = UUID.randomUUID(),
    override val componentPath: String,
    val signalName: String
) : TraceEvent