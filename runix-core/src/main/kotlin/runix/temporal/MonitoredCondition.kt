package runix.temporal

import kotlinx.coroutines.flow.StateFlow

/** Given a boolean flow, returns its unique tracker key. */
internal typealias FlowKeyProvider = (StateFlow<Boolean>) -> String

/**
 * The result of compiling a condition:
 *  - the runtime evaluator
 *  - the list of flow-to-tracker registrations
 */
internal typealias ConditionCompilation = Pair<TemporalExpression, List<FlowRegistration>>

/**
 * A declarative, composable boolean condition over one or more [StateFlow] signals.
 *
 * The framework:
 * 1. Calls [compile] with a naming function to obtain a [TemporalExpression] and
 *    list of [FlowRegistration]s.
 * 2. Registers each flow under the generated key.
 * 3. Evaluates the compiled expression at runtime to produce [ConditionEval] results.
 */
sealed class MonitoredCondition {

    /**
     * All [StateFlow]s that this condition watches.
     *
     * Internal to the framework: used when setting up trackers.
     */
    internal abstract fun flows(): Set<StateFlow<Boolean>>

    /**
     * Compiles this condition into a [TemporalExpression] plus the
     * list of [FlowRegistration]s needed by the engine.
     *
     * @param getKey Function that assigns a unique tracker key per flow.
     * @return A Pair of (runtime evaluator, flow registrations).
     */
    internal abstract fun compile(getKey: FlowKeyProvider): ConditionCompilation

    /**
     * Framework entry point for turning this condition into a [CompiledMonitor].
     *
     * @param monitorName Logical name under which all involved flows are registered.
     * @return A [CompiledMonitor] ready for execution by the engine.
     */
    internal fun compile(monitorName: String): CompiledMonitor {
        val keyMap = mutableMapOf<StateFlow<Boolean>, String>()
        var counter = 0

        // Assign a stable but unique key per flow:
        val getKey: FlowKeyProvider = { flow ->
            keyMap.getOrPut(flow) { "$monitorName::$counter" }
                .also { counter++ }
        }

        // Delegate to the internal compile(getKey) to build the expression & regs
        val (expr, regs) = compile(getKey)
        return CompiledMonitor(
            name = monitorName,
            compiledCondition = expr,
            flowRegistrations = regs
        )
    }

    // ------------------------------------------------------------------------
    // Concrete condition types
    // ------------------------------------------------------------------------

    /**
     * A single boolean signal checked against one [ConditionType].
     */
    data class Leaf(
        private val flow: StateFlow<Boolean>,
        private val type:    ConditionType
    ) : MonitoredCondition() {
        override fun flows(): Set<StateFlow<Boolean>> =
            setOf(flow)

        override fun compile(getKey: FlowKeyProvider): ConditionCompilation {
            val key = getKey(flow)
            val expr = type.compileExpression(key)
            val reg = FlowRegistration(flow, key, type.retentionWindow)
            return expr to listOf(reg)
        }
    }

    /**
     * Logical AND of multiple sub-conditions.
     * True only if *all* parts evaluate to True.
     */
    data class AllOf(private val parts: List<MonitoredCondition>) : MonitoredCondition() {
        override fun flows(): Set<StateFlow<Boolean>> =
            parts.flatMap { it.flows() }.toSet()

        override fun compile(getKey: FlowKeyProvider): ConditionCompilation {
            val compiled = parts.map { it.compile(getKey) }
            val exprs = compiled.map { it.first }
            val regs = compiled.flatMap { it.second }

            val combined: TemporalExpression = {
                val results = exprs.map { it() }
                ConditionEval.mergeAll(results)
            }
            return combined to regs
        }
    }

    /**
     * Logical OR of multiple sub-conditions.
     * True if *any* part evaluates to True.
     */
    data class AnyOf(private val parts: List<MonitoredCondition>) : MonitoredCondition() {
        override fun flows(): Set<StateFlow<Boolean>> =
            parts.flatMap { it.flows() }.toSet()

        override fun compile(getKey: FlowKeyProvider): ConditionCompilation {
            val compiled = parts.map { it.compile(getKey) }
            val exprs = compiled.map { it.first }
            val regs = compiled.flatMap { it.second }

            val combined: TemporalExpression = {
                val results = exprs.map { it() }
                ConditionEval.mergeAny(results)
            }
            return combined to regs
        }
    }

    /**
     * Logical NOT (negation) of a sub-condition.
     * True if the inner part evaluates to False.
     */
    data class Not(private val part: MonitoredCondition) : MonitoredCondition() {
        override fun flows(): Set<StateFlow<Boolean>> =
            part.flows()

        override fun compile(getKey: FlowKeyProvider): ConditionCompilation {
            val (expr, regs) = part.compile(getKey)
            val negated: TemporalExpression = { expr().invert() }
            return negated to regs
        }
    }
}