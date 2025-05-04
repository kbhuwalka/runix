package runix.temporal.dsl

import kotlinx.coroutines.flow.StateFlow
import runix.primitives.LabeledFlow
import runix.temporal.BooleanLeaf
import runix.temporal.MonitoredCondition
import runix.temporal.condition.HasFluctuated
import runix.temporal.condition.HasPersistedTrue
import runix.temporal.condition.IsTrue
import runix.temporal.condition.WasEverTrue
import runix.temporal.condition.WasTrueFor
import kotlin.time.Duration

/**
 * Boolean cognition extensions for [StateFlow] and [LabeledFlow].
 *
 * These extensions enable developers to express time-aware Boolean conditions
 * declaratively. The resulting [MonitoredCondition]s can be composed and evaluated
 * over time using the monitor system.
 *
 * Each function models a different kind of cognitive contract:
 * - Instant checks (`isTrue()`)
 * - Time-held checks (`hasBeenTrueFor(...)`)
 * - Historical checks (`wasTrueFor(...)`, `wasEverTrue(...)`)
 * - Fluctuation detection (`hasFluctuated(...)`)
 *
 * All time-based conditions are retention-aware and bounded in memory.
 */

/**
 * Creates a condition that is true when the current Boolean value is `true`.
 *
 * This is a memoryless, instantaneous snapshot of the current value.
 *
 * Use when:
 * - You want to trigger behavior based on a current flag or sensor being `true`
 *
 * Example:
 * ```
 * doorSensor.isTrue()
 * ```
 */
fun StateFlow<Boolean>.isTrue(): MonitoredCondition =
    BooleanLeaf(this, IsTrue())

/**
 * @see [StateFlow.isTrue]
 */
fun LabeledFlow<Boolean>.isTrue(): MonitoredCondition =
    BooleanLeaf(flow, IsTrue())

/**
 * Creates a condition that is true only if the Boolean value has remained `true`
 * continuously for the specified [forDuration].
 *
 * This uses memory — it models persistence, not just a snapshot.
 *
 * Use when:
 * - You want to detect a signal that has held true (e.g., “alarm held for 5 seconds”)
 *
 * @param forDuration The minimum time the value must remain true
 *
 * Example:
 * ```
 * doorSensor.hasBeenTrueFor(10.seconds)
 * ```
 */
fun StateFlow<Boolean>.hasBeenTrueFor(forDuration: Duration): MonitoredCondition =
    BooleanLeaf(this, HasPersistedTrue(forDuration))

/**
 * @see [StateFlow.hasBeenTrueFor]
 */
fun LabeledFlow<Boolean>.hasBeenTrueFor(forDuration: Duration): MonitoredCondition =
    BooleanLeaf(flow, HasPersistedTrue(forDuration))

/**
 * Creates a condition that is true if the Boolean value was `true` for at least [forDuration],
 * at any point within the last [inLast] time window.
 *
 * This models historical memory — whether a condition held for long enough in the past.
 *
 * Use when:
 * - You want to know if a signal was sustained earlier (e.g., “warning light held for 10s in the last hour”)
 *
 * @param forDuration How long the value must have held true
 * @param inLast How far back to look in memory
 */
fun StateFlow<Boolean>.wasTrueFor(forDuration: Duration, inLast: Duration): MonitoredCondition =
    BooleanLeaf(this, WasTrueFor(forDuration, inLast))

/**
 * @see [StateFlow.wasTrueFor]
 */
fun LabeledFlow<Boolean>.wasTrueFor(forDuration: Duration, inLast: Duration): MonitoredCondition =
    BooleanLeaf(flow, WasTrueFor(forDuration, inLast))

/**
 * Creates a condition that is true if the value was `true` at any time
 * within the last [inLast] window, regardless of how long it lasted.
 *
 * This models memory of transient truth — even if brief.
 *
 * Use when:
 * - You want to catch flickers or brief triggers (e.g., “door was opened in the last 10s”)
 *
 * @param inLast The time window to search backward
 */
fun StateFlow<Boolean>.wasEverTrue(inLast: Duration): MonitoredCondition =
    BooleanLeaf(this, WasEverTrue(inLast))

/**
 * @see [StateFlow.wasEverTrue]
 */
fun LabeledFlow<Boolean>.wasEverTrue(inLast: Duration): MonitoredCondition =
    BooleanLeaf(flow, WasEverTrue(inLast))

/**
 * Creates a condition that is true if the Boolean value changed
 * at least once within the last [inLast] window.
 *
 * This is useful for detecting instability, toggles, or bouncing signals.
 *
 * Use when:
 * - You want to respond to signal volatility (e.g., “motion detector fluctuated recently”)
 *
 * @param inLast The time window to track signal changes
 */
fun StateFlow<Boolean>.hasFluctuated(inLast: Duration): MonitoredCondition =
    BooleanLeaf(this, HasFluctuated(inLast))

/**
 * @see [StateFlow.hasFluctuated]
 */
fun LabeledFlow<Boolean>.hasFluctuated(inLast: Duration): MonitoredCondition =
    BooleanLeaf(flow, HasFluctuated(inLast))
