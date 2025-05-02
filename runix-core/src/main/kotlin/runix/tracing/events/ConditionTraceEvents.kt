package runix.tracing.events

import java.time.Instant
import java.util.UUID

/**
 * Emitted when a Condition is evaluated and the result changes.
 * This is emitted only if the evaluation contributes to a Monitor triggering
 * or is manually invoked by a developer for inspection.
 */
data class ConditionEvaluated(
    override val timestamp: Instant = Instant.now(),
    override val traceId: UUID = UUID.randomUUID(),
    override val componentPath: String,
    val conditionName: String,
    val result: Boolean,
    val reason: String? = null,
    val involvedTrackers: Map<String, Any?> = emptyMap()
) : TraceEvent