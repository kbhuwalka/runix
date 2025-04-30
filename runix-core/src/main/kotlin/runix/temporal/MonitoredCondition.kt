package runix.temporal

import runix.temporal.condition.TemporalExpression

/**
 * A declarative, composable condition built from one or more signals.
 *
 * Conditions describe *what should be true* — not *how to observe it*.
 * They are compiled into executable monitors that track signal changes over time
 * and evaluate changes when necessary.
 *
 * Use `monitor(name)` to compile and start a condition.
 *
 * Compose conditions using `allOf(...)`, `anyOf(...)`, and `not(...)`, or bind them to individual signals:
 *
 * ```
 * monitor("cooling-trigger") {
 *   when(
 *     allOf(
 *       motor.isRunning.isTrue(),
 *       temperature.isAbove(80.0, forTime = 30.seconds)
 *     )
 *   )
 *   emit { fan.turnOn() }
 * }
 * ```
 *
 * All signal registration, flow observation, and history management are handled by the framework.
 *
 * You can compile a condition into an executable monitor by calling [compile].
 */
abstract class MonitoredCondition {

    internal class KeyAllocator(private val monitorName: String) {
        private var counter = 0
        fun nextKey(): String = "$monitorName::${counter++}"
    }

    /**
     * Compiles this condition into a [CompiledMonitor] with a unique monitor name.
     *
     * This method:
     * - Allocates unique internal tracker keys
     * - Prepares signal tracking metadata
     * - Returns a ready-to-start monitor instance
     *
     * This does not begin evaluation — call [CompiledMonitor.start] to activate it.
     *
     * @param monitorName A developer-visible name for this monitor (used for tracing and registration).
     * @return A compiled monitor that can be started, stopped, and evaluated.
     */
    internal fun compile(monitorName: String): CompiledMonitor {
        val bindings = mutableListOf<FlowBinding<*>>()
        val keyAllocator = KeyAllocator(monitorName)
        val expression = build(bindings, keyAllocator)
        return CompiledMonitor(
            name = monitorName,
            condition = expression,
            bindings = bindings)
    }

    /**
     * Subclasses must implement this to build their condition's expression tree
     * and collect the associated signal bindings.
     *
     * This method is internal to the framework and should not be used directly by developers.
     *
     * @param bindings Output list that should be populated with all flow bindings for this condition.
     * @param keyAllocator Provides a unique key per signal reference used in the condition.
     * @return A [TemporalExpression] that evaluates the condition at runtime.
     */
    internal abstract fun build(
        bindings: MutableList<FlowBinding<*>>,
        keyAllocator: KeyAllocator
    ): TemporalExpression
}