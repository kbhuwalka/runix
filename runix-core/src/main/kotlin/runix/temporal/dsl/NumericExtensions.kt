package runix.temporal.dsl

import kotlinx.coroutines.flow.StateFlow
import runix.primitives.LabeledFlow
import runix.temporal.MonitoredCondition
import runix.temporal.NumericLeaf
import runix.temporal.condition.Decreasing
import runix.temporal.condition.FluctuatedBeyond
import runix.temporal.condition.HasPersistedAbove
import runix.temporal.condition.HasPersistedBelow
import runix.temporal.condition.Increasing
import runix.temporal.condition.IsAbove
import runix.temporal.condition.IsBelow
import runix.temporal.condition.StableWithin
import runix.temporal.condition.WasAboveFor
import runix.temporal.condition.WasBelowFor
import runix.temporal.condition.WasEverAbove
import runix.temporal.condition.WasEverBelow
import kotlin.time.Duration

/**
 * Numeric cognition extensions for [StateFlow] and [LabeledFlow] of [Double].
 *
 * These extensions allow developers to express time-aware numeric conditions declaratively,
 * with memory-bounded evaluations that reflect real-world behaviors.
 *
 * Each condition is retention-aware and declaratively models one of:
 * - Snapshot comparisons (e.g., `.isAbove(...)`)
 * - Persistence of value (e.g., `.hasBeenAboveFor(...)`)
 * - Historical dwell (e.g., `.wasBelow(...)`)
 * - Volatility and fluctuation (e.g., `.hasFluctuatedBeyond(...)`)
 * - Trends over time (e.g., `.isIncreasing(...)`)
 */

// ─────────────────────────────────────────────────────────────
// Point-in-time checks
// ─────────────────────────────────────────────────────────────

/**
 * Returns `True` if the current value is strictly greater than [threshold].
 *
 * Snapshot — memoryless
 * Use this when:
 * - You want to evaluate the signal's real-time numeric value
 * - You only care about the current reading, not its history
 *
 * @param threshold The comparison value
 *
 * Example:
 * ```
 * temperature.isAbove(80.0)
 * ```
 */
fun StateFlow<Double>.isAbove(threshold: Double): MonitoredCondition =
    NumericLeaf(this, IsAbove(threshold))

/**
 * @see [StateFlow.isAbove]
 */
fun LabeledFlow<Double>.isAbove(threshold: Double): MonitoredCondition =
    NumericLeaf(flow, IsAbove(threshold))

/**
 * Returns `True` if the current value is strictly less than [threshold].
 *
 * Snapshot — memoryless
 * Use this when:
 * - You need to detect dips or low readings in real time
 * - You do not require any historical retention or dwell logic
 *
 * @param threshold The comparison value
 *
 * Example:
 * ```
 * batteryLevel.isBelow(15.0)
 * ```
 */
fun StateFlow<Double>.isBelow(threshold: Double): MonitoredCondition =
    NumericLeaf(this, IsBelow(threshold))

/**
 * @see [StateFlow.isBelow]
 */
fun LabeledFlow<Double>.isBelow(threshold: Double): MonitoredCondition =
    NumericLeaf(flow, IsBelow(threshold))

// ─────────────────────────────────────────────────────────────
// Persistent (live) checks
// ─────────────────────────────────────────────────────────────

/**
 * Returns `True` if the current value has remained strictly above [threshold]
 * continuously for at least [forDuration].
 *
 * Persistent — retention-aware, time must accumulate.
 * Use this when:
 * - You want to ensure a signal is *still* above a threshold and has been for a minimum time
 * - You're guarding against transient spikes
 *
 * @param threshold The comparison value
 * @param forDuration How long the condition must have held (dwell time)
 *
 * Example:
 * ```
 * temperature.hasBeenAboveFor(80.0, forDuration = 30.seconds)
 * ```
 */
fun StateFlow<Double>.hasBeenAboveFor(threshold: Double, forDuration: Duration): MonitoredCondition =
    NumericLeaf(this, HasPersistedAbove(threshold, forDuration))

/**
 * @see [StateFlow.hasBeenAboveFor]
 */
fun LabeledFlow<Double>.hasBeenAboveFor(threshold: Double, forDuration: Duration): MonitoredCondition =
    NumericLeaf(flow, HasPersistedAbove(threshold, forDuration))

/**
 * Returns `True` if the current value has remained strictly below [threshold]
 * continuously for at least [forDuration].
 *
 * Persistent — retention-aware, time must accumulate.
 * Use this when:
 * - You need to confirm a stable low value before triggering an action
 * - You're debouncing against noise in readings
 *
 * @param threshold The comparison value
 * @param forDuration How long the condition must have held (dwell time)
 *
 * Example:
 * ```
 * pressure.hasBeenBelowFor(20.0, forDuration = 10.seconds)
 * ```
 */
fun StateFlow<Double>.hasBeenBelowFor(threshold: Double, forDuration: Duration): MonitoredCondition =
    NumericLeaf(this, HasPersistedBelow(threshold, forDuration))

/**
 * @see [StateFlow.hasBeenBelowFor]
 */
fun LabeledFlow<Double>.hasBeenBelowFor(threshold: Double, forDuration: Duration): MonitoredCondition =
    NumericLeaf(flow, HasPersistedBelow(threshold, forDuration))

// ─────────────────────────────────────────────────────────────
// Historical dwell (dual-time) checks
// ─────────────────────────────────────────────────────────────

/**
 * Returns `True` if the value was above [threshold] for at least [forDuration]
 * at any point within the last [inLast] window.
 *
 * Historical — retention-aware, time must accumulate.
 * Use this when:
 * - You care about whether a condition held sometime recently
 * - You're validating patterns like "was overheated at some point"
 *
 * @param threshold The comparison value
 * @param forDuration How long the condition must have held (dwell time)
 * @param inLast The window in which the crossing must have occurred
 *
 * Example:
 * ```
 * temperature.wasAbove(85.0, forDuration = 30.seconds, inLast = 5.minutes)
 * ```
 */
fun StateFlow<Double>.wasAbove(threshold: Double, forDuration: Duration, inLast: Duration): MonitoredCondition =
    NumericLeaf(this, WasAboveFor(threshold, forDuration, inLast))

/**
 * @see [StateFlow.wasAbove]
 */
fun LabeledFlow<Double>.wasAbove(threshold: Double, forDuration: Duration, inLast: Duration): MonitoredCondition =
    NumericLeaf(flow, WasAboveFor(threshold, forDuration, inLast))

/**
 * Returns `True` if the value was below [threshold] for at least [forDuration]
 * within the past [inLast] window.
 *
 * Historical — retention-aware, time must accumulate.
 * Use this when:
 * - You're looking for prior dips or low states that occurred recently
 *
 * @param threshold The comparison value
 * @param forDuration How long the condition must have held (dwell time)
 * @param inLast The window in which the crossing must have occurred
 *
 * Example:
 * ```
 * cpuUsage.wasBelow(10.0, forDuration = 5.seconds, inLast = 1.minute)
 * ```
 */
fun StateFlow<Double>.wasBelow(threshold: Double, forDuration: Duration, inLast: Duration): MonitoredCondition =
    NumericLeaf(this, WasBelowFor(threshold, forDuration, inLast))

/**
 * @see [StateFlow.wasBelow]
 */
fun LabeledFlow<Double>.wasBelow(threshold: Double, forDuration: Duration, inLast: Duration): MonitoredCondition =
    NumericLeaf(flow, WasBelowFor(threshold, forDuration, inLast))

// ─────────────────────────────────────────────────────────────
// Historical memory (any dwell length)
// ─────────────────────────────────────────────────────────────

/**
 * Returns `True` if the value exceeded [threshold] at any point in the last [inLast] window.
 *
 * Historical blip — no dwell time required
 * Use this when:
 * - You want to detect temporary spikes — even if they didn't persist
 *
 * @param threshold The comparison value
 * @param inLast The window in which the crossing must have occurred
 *
 * Example:
 * ```
 * temperature.wasEverAbove(100.0, inLast = 2.minutes)
 * ```
 */
fun StateFlow<Double>.wasEverAbove(threshold: Double, inLast: Duration): MonitoredCondition =
    NumericLeaf(this, WasEverAbove(threshold, inLast))

/**
 * @see [StateFlow.wasEverAbove]
 */
fun LabeledFlow<Double>.wasEverAbove(threshold: Double, inLast: Duration): MonitoredCondition =
    NumericLeaf(flow, WasEverAbove(threshold, inLast))

/**
 * Returns `True` if the value dropped below [threshold] at any point in the last [inLast] window.
 *
 * Historical blip — no dwell time required
 * Use this when:
 * - You want to detect temporary dips or drops — even if they didn't persist
 *
 * @param threshold The comparison value
 * @param inLast The window in which the crossing must have occurred
 *
 * Example:
 * ```
 * voltage.wasEverBelow(3.0, inLast = 30.seconds)
 * ```
 */
fun StateFlow<Double>.wasEverBelow(threshold: Double, inLast: Duration): MonitoredCondition =
    NumericLeaf(this, WasEverBelow(threshold, inLast))

/**
 * @see [StateFlow.wasEverBelow]
 */
fun LabeledFlow<Double>.wasEverBelow(threshold: Double, inLast: Duration): MonitoredCondition =
    NumericLeaf(flow, WasEverBelow(threshold, inLast))

/**
 * Returns `True` if the value has been monotonically increasing
 * for at least [forDuration].
 *
 * Trend detection — retention-aware
 * Use this when:
 * - You want to confirm a rising signal over time (e.g., ramp-up)
 *
 * @param forDuration The minimum duration of the continuous increase
 *
 * Example:
 * ```
 * fanSpeed.isIncreasing(forDuration = 15.seconds)
 * ```
 */
fun StateFlow<Double>.isIncreasing(forDuration: Duration): MonitoredCondition =
    NumericLeaf(this, Increasing(forDuration))

/**
 * @see [StateFlow.isIncreasing]
 */
fun LabeledFlow<Double>.isIncreasing(forDuration: Duration): MonitoredCondition =
    NumericLeaf(flow, Increasing(forDuration))

/**
 * Returns `True` if the value has been monotonically decreasing
 * for at least [forDuration].
 *
 * Trend detection — retention-aware
 * Use this when:
 * - You need to verify consistent downward movement (e.g., cooling behavior)
 *
 * @param forDuration The minimum duration of the continuous decrease
 *
 * Example:
 * ```
 * temperature.isDecreasing(forDuration = 10.seconds)
 * ```
 */
fun StateFlow<Double>.isDecreasing(forDuration: Duration): MonitoredCondition =
    NumericLeaf(this, Decreasing(forDuration))

/**
 * @see [StateFlow.isDecreasing]
 */
fun LabeledFlow<Double>.isDecreasing(forDuration: Duration): MonitoredCondition =
    NumericLeaf(flow, Decreasing(forDuration))

/**
 * Returns `True` if the signal has remained stable — i.e., the spread between
 * min and max values has stayed within [margin] — within the last [inLast] window
 *
 * Stability window — retention-aware
 * Use this when:
 * - You need to ensure a numeric signal isn't fluctuating beyond acceptable noise
 *
 * @param margin The maximum allowed range between min and max values
 * @param inLast Duration of required stability
 *
 * Example:
 * ```
 * rpm.isStableWithin(margin = 5.0, forDuration = 20.seconds)
 * ```
 */
fun StateFlow<Double>.isStableWithin(margin: Double, inLast: Duration): MonitoredCondition =
    NumericLeaf(this, StableWithin(margin, inLast))

/**
 * @see [StateFlow.isStableWithin]
 */
fun LabeledFlow<Double>.isStableWithin(margin: Double, inLast: Duration): MonitoredCondition =
    NumericLeaf(flow, StableWithin(margin, inLast))

/**
 * Returns `True` if the value has fluctuated beyond [margin] at any point
 * in the last [inLast] window — i.e., a spike or volatility was observed.
 *
 * Volatility detection — retention-aware
 * Use this when:
 * - You want to react to spikes or erratic behavior, even briefly
 *
 * @param margin The maximum allowed range between min and max values
 * @param inLast The time window in which to detect the volatility
 *
 * Example:
 * ```
 * temperature.hasFluctuatedBeyond(margin = 3.0, inLast = 1.minute)
 * ```
 */
fun StateFlow<Double>.hasFluctuatedBeyond(margin: Double, inLast: Duration): MonitoredCondition =
    NumericLeaf(this, FluctuatedBeyond(margin, inLast))

/**
 * @see [StateFlow.hasFluctuatedBeyond]
 */
fun LabeledFlow<Double>.hasFluctuatedBeyond(margin: Double, inLast: Duration): MonitoredCondition =
    NumericLeaf(flow, FluctuatedBeyond(margin, inLast))
