package runix.temporal

import kotlinx.coroutines.flow.StateFlow

sealed class MonitoredCondition {
    abstract fun flows(): Set<StateFlow<Boolean>>

    data class Leaf(
        val flow: StateFlow<Boolean>,
        val type: TemporalType
    ) : MonitoredCondition() {
        override fun flows(): Set<StateFlow<Boolean>> = setOf(flow)
    }

    data class Not(val inner: MonitoredCondition) : MonitoredCondition() {
        override fun flows(): Set<StateFlow<Boolean>> = inner.flows()
    }

    data class AllOf(val parts: List<MonitoredCondition>) : MonitoredCondition() {
        override fun flows(): Set<StateFlow<Boolean>> = parts.flatMap { it.flows() }.toSet()
    }

    data class AnyOf(val parts: List<MonitoredCondition>) : MonitoredCondition() {
        override fun flows(): Set<StateFlow<Boolean>> = parts.flatMap { it.flows() }.toSet()
    }
}

fun MonitoredCondition.compile(
    keys: Map<StateFlow<Boolean>, String>
): TemporalExpression {
    return when (this) {
        is MonitoredCondition.Leaf -> {
            val key = keys[flow]
                ?: error("Missing key for StateFlow in Leaf")

            val tracker = TemporalEngine.getBooleanTracker(key)

            when (type) {
                is TemporalType.Instant -> tracker.compileWhenTrue()
                is TemporalType.Persisted -> tracker.compilePersistedFor(type.duration)
                is TemporalType.WasStable -> tracker.compileWasStableFor(type.duration)
            }
        }

        is MonitoredCondition.Not -> {
            val innerExpr = inner.compile(keys)
            return {
                when (val result = innerExpr()) {
                    is ConditionEval.True -> ConditionEval.False
                    is ConditionEval.False -> ConditionEval.True
                    is ConditionEval.Delayed -> result
                }
            }
        }

        is MonitoredCondition.AllOf -> {
            val compiled = parts.map { it.compile(keys) }
            allOf(*compiled.toTypedArray())
        }

        is MonitoredCondition.AnyOf -> {
            val compiled = parts.map { it.compile(keys) }
            anyOf(*compiled.toTypedArray())
        }
    }
}
