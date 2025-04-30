package runix.temporal.dsl

import runix.temporal.MonitoredCondition
import runix.temporal.AllOf
import runix.temporal.AnyOf
import runix.temporal.Not

/**
 * Combines multiple [MonitoredCondition]s using logical AND.
 *
 * Returns `True` only when **all** of the provided conditions return `True`.
 *
 * Use this to:
 * - Require multiple signal states to be true together
 * - Create conjunctive cognitive models
 *
 * Example:
 * ```
 * allOf(
 *     motor.isTrue(),
 *     temperature.isBelow(80.0),
 *     doorSensor.wasEverTrue(10.seconds)
 * )
 * ```
 *
 * @param conditions The list of conditions to combine
 * @return A composite condition that succeeds when all sub-conditions succeed
 */
fun allOf(vararg conditions: MonitoredCondition): MonitoredCondition =
    AllOf(conditions.toList())

/**
 * Combines multiple [MonitoredCondition]s using logical OR.
 *
 * Returns `True` if **any** of the provided conditions return `True`.
 *
 * Use this to:
 * - Accept multiple possible inputs
 * - Handle fallback or override logic
 *
 * Example:
 * ```
 * anyOf(
 *     overrideSwitch.isTrue(),
 *     temperature.isAbove(90.0),
 *     safetyCheck.wasTrueFor(3.seconds, inLast = 30.seconds)
 * )
 * ```
 *
 * @param conditions The list of conditions to combine
 * @return A composite condition that succeeds when at least one sub-condition succeeds
 */
fun anyOf(vararg conditions: MonitoredCondition): MonitoredCondition =
    AnyOf(conditions.toList())

/**
 * Negates the result of a single [MonitoredCondition].
 *
 * Returns `True` when the provided condition is `False`, and vice versa.
 * A `Delayed` result remains unchanged.
 *
 * Use this to:
 * - Express "not" logic cleanly
 * - Invert a condition declaratively
 *
 * Example:
 * ```
 * not(doorSensor.isTrue())
 * ```
 *
 * @param condition The condition to negate
 * @return A logical NOT of the input condition
 */
fun not(condition: MonitoredCondition): MonitoredCondition =
    Not(condition)