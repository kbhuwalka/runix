package runix.temporal

import kotlinx.coroutines.flow.StateFlow
import runix.temporal.condition.BooleanConditionType
import runix.temporal.condition.CategoricalConditionType
import runix.temporal.condition.NumericConditionType
import runix.temporal.condition.TemporalExpression
import runix.temporal.trackers.BooleanTracker
import runix.temporal.trackers.CategoricalTracker
import runix.temporal.trackers.NumericTracker
import runix.temporal.trackers.TrackerRegistry

/**
 * Leaf condition nodes for signal evaluation.
 *
 * Each leaf binds a specific signal (Boolean, Numeric, or Categorical) to a declarative [ConditionType],
 * and participates in the monitor compilation process.
 *
 * Leaf nodes are created via signal DSLs like `.isTrue()`, `.isAbove(...)`, or `.isInState(...)`.
 */

internal class BooleanLeaf(
    private val flow: StateFlow<Boolean>,
    private val type: BooleanConditionType
) : MonitoredCondition() {
    override fun build(
        bindings: MutableList<FlowBinding<*>>,
        keyAllocator: KeyAllocator
    ): TemporalExpression {
        val key = keyAllocator.nextKey()
        bindings += FlowBinding(
            key = key,
            retention = type.retention,
            tracker = BooleanTracker(flow, type.retention)
        )
        return type.compileExpression(key, TrackerRegistry)
    }
}

internal class NumericLeaf(
    private val flow: StateFlow<Double>,
    private val type: NumericConditionType
) : MonitoredCondition() {
    override fun build(
        bindings: MutableList<FlowBinding<*>>,
        keyAllocator: KeyAllocator
    ): TemporalExpression {
        val key = keyAllocator.nextKey()
        bindings += FlowBinding(
            key = key,
            retention = type.retention,
            tracker = NumericTracker(flow, type.retention)
        )
        return type.compileExpression(key, TrackerRegistry)
    }
}

internal class CategoricalLeaf<T>(
    private val flow: StateFlow<T>,
    private val type: CategoricalConditionType
) : MonitoredCondition() {
    override fun build(
        bindings: MutableList<FlowBinding<*>>,
        keyAllocator: KeyAllocator
    ): TemporalExpression {
        val key = keyAllocator.nextKey()
        bindings += FlowBinding(
            key = key,
            retention = type.retention,
            tracker = CategoricalTracker(flow, type.retention)
        )
        return type.compileExpression(key, TrackerRegistry)
    }
}
