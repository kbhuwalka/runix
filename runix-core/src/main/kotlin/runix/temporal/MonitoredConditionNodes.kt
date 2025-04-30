package runix.temporal

import runix.temporal.condition.ConditionEval
import runix.temporal.condition.TemporalExpression

/**
 * A composite condition that returns `True` only if **all** of its sub-conditions return `True`.
 *
 * This models logical AND over multiple [MonitoredCondition]s.
 *
 * - If any condition returns `False`, the result is `False`
 * - If any condition is `Delayed`, and none are `False`, the result is `Delayed`
 * - If all conditions return `True`, the result is `True`
 *
 * Use this when:
 * - You want to require multiple signals or predicates to hold together
 * - You're combining state + sensor + time conditions into a single decision point
 *
 * Example:
 * ```
 * allOf(
 *   motor.isTrue(),
 *   temperature.isBelow(70.0),
 *   doorSensor.wasEverTrue(10.seconds)
 * )
 * ```
 */
internal class AllOf(
    private val parts: List<MonitoredCondition>
) : MonitoredCondition() {
    override fun build(
        bindings: MutableList<FlowBinding<*>>,
        keyAllocator: KeyAllocator
    ): TemporalExpression {
        val expressions = parts.map { it.build(bindings, keyAllocator) }
        return { ConditionEval.mergeAll(expressions.map { it() }) }
    }
}

/**
 * A composite condition that returns `True` if **any** of its sub-conditions return `True`.
 *
 * This models logical OR over multiple [MonitoredCondition]s.
 *
 * - If at least one condition returns `True`, the result is `True`
 * - If all conditions are `False`, the result is `False`
 * - If at least one is `Delayed` and none are `True`, the result is `Delayed`
 *
 * Use this when:
 * - You want to create fallback or override logic
 * - You want to model “satisfy any of these paths” cognition
 *
 * Example:
 * ```
 * anyOf(
 *   overrideSwitch.isTrue(),
 *   temperature.wasAbove(90.0, 5.seconds, inLast = 1.minute),
 *   safetyLatch.transitionedTo(OPEN, inLast = 30.seconds)
 * )
 * ```
 */
internal class AnyOf(
    private val parts: List<MonitoredCondition>
) : MonitoredCondition() {
    override fun build(
        bindings: MutableList<FlowBinding<*>>,
        keyAllocator: KeyAllocator
    ): TemporalExpression {
        val expressions = parts.map { it.build(bindings, keyAllocator) }
        return { ConditionEval.mergeAny(expressions.map { it() }) }
    }
}

/**
 * A condition that inverts the result of a single [MonitoredCondition].
 *
 * - `True` becomes `False`
 * - `False` becomes `True`
 * - `Delayed` remains `Delayed`
 *
 * Use this when:
 * - You want to express negation clearly and declaratively
 * - You're modeling “not in this state” or “not triggered yet”
 *
 * Example:
 * ```
 * not(doorSensor.isTrue())
 * ```
 */
internal class Not(
    private val part: MonitoredCondition
) : MonitoredCondition() {
    override fun build(
        bindings: MutableList<FlowBinding<*>>,
        keyAllocator: KeyAllocator
    ): TemporalExpression {
        val expr = part.build(bindings, keyAllocator)
        return { expr().invert() }
    }
}