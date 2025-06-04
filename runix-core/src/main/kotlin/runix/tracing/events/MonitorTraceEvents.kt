package runix.tracing.events

import java.time.Instant
import java.util.UUID

/**
 * Emitted when a Monitor's compositional condition becomes true and fires.
 * This is the key moment of evaluation → action transition.
 */
internal data class MonitorTriggered(
    override val timestamp: Instant = Instant.now(),
    override val traceId: UUID = UUID.randomUUID(),
    override val parent: TraceEvent?,
    val monitorName: String
) : TraceEvent {
    override fun toString(): String {
        return "${this.javaClass.simpleName}($monitorName): [${traceId.toString().take(8)}]"
    }
}

/**
 * Emitted when a Monitor emits a signal due to its triggering condition.
 * This is always immediately preceded by a MonitorTriggered event.
 */
internal data class SignalEmitted(
    override val timestamp: Instant = Instant.now(),
    override val traceId: UUID = UUID.randomUUID(),
    override val parent: TraceEvent?,
    val signalName: String
) : TraceEvent {
    override fun toString(): String {
        return "${this.javaClass.simpleName}($signalName): [${traceId.toString().take(8)}]"
    }
}
