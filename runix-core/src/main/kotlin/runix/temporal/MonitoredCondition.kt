package runix.temporal

import kotlinx.coroutines.flow.StateFlow

sealed class MonitoredCondition {
    abstract fun flows(): Set<StateFlow<Boolean>>
    abstract fun compileInternal(getKey: (StateFlow<Boolean>) -> String): Pair<TemporalExpression, List<FlowRegistration>>

    fun compileForRegistration(monitorName: String): CompiledMonitor {
        val keyMap = mutableMapOf<StateFlow<Boolean>, String>()
        var counter = 0

        // Lambda to generate or retrieve keys per flow
        val getKey: (StateFlow<Boolean>) -> String = { flow ->
            keyMap.getOrPut(flow) { "${monitorName}::$counter" }.also { counter++ }
        }

        val (compiledExpression, flowRegistrations) = this.compileInternal(getKey)

        return CompiledMonitor(
            compiledCondition = compiledExpression,
            flowRegistrations = flowRegistrations
        )
    }

    data class Leaf(
        val flow: StateFlow<Boolean>,
        val type: TemporalType
    ) : MonitoredCondition() {
        override fun flows(): Set<StateFlow<Boolean>> = setOf(flow)

        override fun compileInternal(getKey: (StateFlow<Boolean>) -> String): Pair<TemporalExpression, List<FlowRegistration>> {
            val key = getKey(flow)
            val retention = type.retentionWindow()
            val compiledExpression = type.compileEvaluation(key)
            return Pair(compiledExpression, listOf(FlowRegistration(flow, key, retention)))
        }
    }

    data class Not(
        val inner: MonitoredCondition
    ) : MonitoredCondition() {
        override fun flows(): Set<StateFlow<Boolean>> = inner.flows()

        override fun compileInternal(getKey: (StateFlow<Boolean>) -> String): Pair<TemporalExpression, List<FlowRegistration>> {
            val (compiledInner, innerFlows) = inner.compileInternal(getKey)

            val compiledExpression: TemporalExpression = {
                compiledInner().invert()
            }

            return Pair(compiledExpression, innerFlows)
        }
    }

    data class AllOf(
        val parts: List<MonitoredCondition>
    ) : MonitoredCondition() {
        override fun flows(): Set<StateFlow<Boolean>> = parts.flatMap { it.flows() }.toSet()

        override fun compileInternal(getKey: (StateFlow<Boolean>) -> String): Pair<TemporalExpression, List<FlowRegistration>> {
            val compiledParts = parts.map { it.compileInternal(getKey) }
            val compiledExpressions = compiledParts.map { it.first }
            val compiledFlows = compiledParts.flatMap { it.second }

            val compiledExpression: TemporalExpression = {
                val results = compiledExpressions.map { it() }
                ConditionEval.mergeAll(results)
            }

            return Pair(compiledExpression, compiledFlows)
        }
    }

    data class AnyOf(
        val parts: List<MonitoredCondition>
    ) : MonitoredCondition() {
        override fun flows(): Set<StateFlow<Boolean>> = parts.flatMap { it.flows() }.toSet()

        override fun compileInternal(getKey: (StateFlow<Boolean>) -> String): Pair<TemporalExpression, List<FlowRegistration>> {
            val compiledParts = parts.map { it.compileInternal(getKey) }
            val compiledExpressions = compiledParts.map { it.first }
            val compiledFlows = compiledParts.flatMap { it.second }

            val compiledExpression: TemporalExpression = {
                val results = compiledExpressions.map { it() }
                ConditionEval.mergeAny(results)
            }

            return Pair(compiledExpression, compiledFlows)
        }
    }
}
