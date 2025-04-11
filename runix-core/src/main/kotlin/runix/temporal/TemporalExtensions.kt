package runix.temporal

import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

fun StateFlow<Boolean>.persistedFor(duration: Duration): MonitoredCondition =
    MonitoredCondition.Leaf(this, TemporalType.Persisted(duration))

fun StateFlow<Boolean>.wasStableFor(duration: Duration): MonitoredCondition =
    MonitoredCondition.Leaf(this, TemporalType.WasStable(duration))

fun StateFlow<Boolean>.whenTrue(): MonitoredCondition =
    MonitoredCondition.Leaf(this, TemporalType.Instant)

fun allOf(vararg parts: MonitoredCondition): MonitoredCondition =
    MonitoredCondition.AllOf(parts.toList())

fun anyOf(vararg parts: MonitoredCondition): MonitoredCondition =
    MonitoredCondition.AnyOf(parts.toList())

fun not(part: MonitoredCondition): MonitoredCondition =
    MonitoredCondition.Not(part)
