package runix.temporal.dsl

import kotlinx.coroutines.flow.StateFlow
import runix.primitives.LabeledFlow
import runix.temporal.CategoricalLeaf
import runix.temporal.MonitoredCondition
import runix.temporal.condition.IsInState
import runix.temporal.condition.PersistedInState
import runix.temporal.condition.TransitionedFrom
import runix.temporal.condition.TransitionedThrough
import runix.temporal.condition.TransitionedTo
import runix.temporal.condition.WasEverInState
import runix.temporal.condition.WasInStateFor
import kotlin.time.Duration

/**
 * Categorical cognition extensions for [StateFlow] and [LabeledFlow] of [Enum].
 *
 * These extensions allow developers to express declarative, time-aware conditions
 * over categorical state values (e.g. robot modes, UI states, finite lifecycles).
 *
 * Categorical conditions can express:
 * - Snapshot state identity (`isIn(...)`)
 * - Persistent state holding (`hasBeenIn(...)`)
 * - Historical occupancy (`wasIn(...)`, `wasEverIn(...)`)
 * - Transitions and dwell (`transitionedTo(...)`, `transitionedFrom(...)`)
 * - Full path sequences (`transitionedThrough(...)`)
 *
 * All operations are memory-bounded and conditionally introspectable.
 */

/**
 * Returns `True` if the current categorical state equals [expected].
 *
 * Snapshot — memoryless
 * Use this when:
 * - You want to check the current state without retention
 *
 * @param expected The target state to compare to the current value
 *
 * Example:
 * ```
 * mode.isIn(RUNNING)
 * ```
 */
fun <T : Enum<T>> StateFlow<T>.isIn(expected: T): MonitoredCondition =
    CategoricalLeaf(this, IsInState(expected))

/**
 * @see StateFlow.isIn
 */
fun <T : Enum<T>> LabeledFlow<T>.isIn(expected: T): MonitoredCondition =
    CategoricalLeaf(flow, IsInState(expected))

/**
 * Returns `True` if the current state has continuously matched [expected]
 * for at least [forDuration].
 *
 * Persistent — retention-aware
 * Use this when:
 * - You want to confirm a state has held for a required duration
 * - You're filtering out brief or flickering states
 *
 * @param expected The target state to persist in
 * @param forDuration How long the value must continuously match [expected]
 */
fun <T : Enum<T>> StateFlow<T>.hasBeenIn(expected: T, forDuration: Duration): MonitoredCondition =
    CategoricalLeaf(this, PersistedInState(expected, forDuration))

/**
 * @see StateFlow.hasBeenIn
 */
fun <T : Enum<T>> LabeledFlow<T>.hasBeenIn(expected: T, forDuration: Duration): MonitoredCondition =
    CategoricalLeaf(flow, PersistedInState(expected, forDuration))

/**
 * Returns `True` if the signal matched [expected] for at least [forDuration]
 * at any point in the last [inLast] window.
 *
 * Historical — retention-aware, time must accumulate.
 * Use this when:
 * - You want to detect a recent period of occupancy in a state
 * - You want to trigger behavior after a state has been held
 *
 * @param expected The state to match historically
 * @param forDuration Duration the state must have been held
 * @param inLast The lookback window for evaluating memory
 */
fun <T : Enum<T>> StateFlow<T>.wasIn(expected: T, forDuration: Duration, inLast: Duration): MonitoredCondition =
    CategoricalLeaf(this, WasInStateFor(expected, forDuration, inLast))

/**
 * @see StateFlow.wasIn
 */
fun <T : Enum<T>> LabeledFlow<T>.wasIn(expected: T, forDuration: Duration, inLast: Duration): MonitoredCondition =
    CategoricalLeaf(flow, WasInStateFor(expected, forDuration, inLast))

/**
 * Returns `True` if the signal was ever equal to [expected]
 * at any time within the last [inLast] window.
 *
 * Historical — retention-aware
 * Use this when:
 * - You want to detect transient state visits
 *
 * @param expected The state to detect
 * @param inLast The window of time in which the state could have occurred
 */
fun <T : Enum<T>> StateFlow<T>.wasEverIn(expected: T, inLast: Duration): MonitoredCondition =
    CategoricalLeaf(this, WasEverInState(expected, inLast))

/**
 * @see StateFlow.wasEverIn
 */
fun <T : Enum<T>> LabeledFlow<T>.wasEverIn(expected: T, inLast: Duration): MonitoredCondition =
    CategoricalLeaf(flow, WasEverInState(expected, inLast))

/**
 * Returns `True` if the signal transitioned into [expected]
 * and remained in that state for at least [forDuration], within the [inLast] window.
 *
 * Causal — transition and dwell
 * Use this when:
 * - You want to detect entry into a state
 * - You want to model causality, not just presence
 *
 * @param expected The destination state
 * @param forDuration Minimum time to remain in the state (default = 0)
 * @param inLast The historical window in which the transition must have occurred
 */
fun <T : Enum<T>> StateFlow<T>.transitionedTo(expected: T, forDuration: Duration = Duration.ZERO, inLast: Duration): MonitoredCondition =
    CategoricalLeaf(this, TransitionedTo(expected, forDuration, inLast))

/**
 * @see StateFlow.transitionedTo
 */
fun <T : Enum<T>> LabeledFlow<T>.transitionedTo(expected: T, forDuration: Duration = Duration.ZERO, inLast: Duration): MonitoredCondition =
    CategoricalLeaf(flow, TransitionedTo(expected, forDuration, inLast))

/**
 * Returns `True` if the signal transitioned away from [expected]
 * and remained in another state for at least [forDuration], within [inLast].
 *
 * Causal — exit transition and hold
 * Use this when:
 * - You want to model recovery or leaving a critical state
 *
 * @param expected The state to transition away from
 * @param forDuration Minimum time to stay outside the state (default = 0)
 * @param inLast The memory window in which to detect the exit
 */
fun <T : Enum<T>> StateFlow<T>.transitionedFrom(expected: T, forDuration: Duration = Duration.ZERO, inLast: Duration): MonitoredCondition =
    CategoricalLeaf(this, TransitionedFrom(expected, forDuration, inLast))

/**
 * @see StateFlow.transitionedFrom
 */
fun <T : Enum<T>> LabeledFlow<T>.transitionedFrom(expected: T, forDuration: Duration = Duration.ZERO, inLast: Duration): MonitoredCondition =
    CategoricalLeaf(flow, TransitionedFrom(expected, forDuration, inLast))

/**
 * Returns `True` if the signal transitioned through the given [states] in exact order
 * at any point within the last [inLast] window.
 *
 * Causal sequence.
 * Use this when:
 * - You want to verify that a cognitive path was followed
 * - The transition order is significant
 *
 * @param states The ordered state sequence to detect
 * @param inLast The lookback window to evaluate the sequence
 */
fun <T : Enum<T>> StateFlow<T>.transitionedThrough(vararg states: T, inLast: Duration): MonitoredCondition =
    CategoricalLeaf(this, TransitionedThrough(states.toList(), inLast))

/**
 * @see StateFlow.transitionedThrough
 */
fun <T : Enum<T>> LabeledFlow<T>.transitionedThrough(vararg states: T, inLast: Duration): MonitoredCondition =
    CategoricalLeaf(flow, TransitionedThrough(states.toList(), inLast))
