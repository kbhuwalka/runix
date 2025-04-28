package runix.temporal

import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

/**
 * DSL extensions for building boolean temporal conditions on a [StateFlow<Boolean>].
 * Provides declarative, time-aware condition builders that can be composed into monitors.
 */

/**
 * Checks if the signal is currently true at this instant.
 *
 * Translates to [ConditionType.IsTrueNow].
 */
fun StateFlow<Boolean>.whenTrue(): MonitoredCondition =
    MonitoredCondition.Leaf(this, ConditionType.IsTrueNow)

/**
 * Checks if the signal has been continuously true for the last [duration].
 *
 * Equivalent to “memoryless” persistence: no intervening false values.
 * Translates to [ConditionType.Persisted].
 *
 * @param duration how long the signal must remain true
 */
fun StateFlow<Boolean>.persistedFor(duration: Duration): MonitoredCondition =
    MonitoredCondition.Leaf(this, ConditionType.Persisted(duration))

/**
 * Checks if the signal has been stable—no flips between true/false—for the last [duration].
 *
 * Useful to detect “quiet” periods on either side.
 * Translates to [ConditionType.WasStable].
 *
 * @param duration how long the signal must remain unchanged
 */
fun StateFlow<Boolean>.wasStableFor(duration: Duration): MonitoredCondition =
    MonitoredCondition.Leaf(this, ConditionType.WasStable(duration))

/**
 * Checks if the signal experienced at least [target] of true time
 * within the last [within] window.
 *
 * Accumulates multiple true spans across the window.
 * Translates to [ConditionType.PersistedForAtLeast].
 *
 * @param target minimum cumulative true duration required
 * @param within time window over which to accumulate
 */
fun StateFlow<Boolean>.persistedForAtLeast(
    target: Duration,
    within: Duration
): MonitoredCondition =
    MonitoredCondition.Leaf(this, ConditionType.PersistedForAtLeast(target, within))

/**
 * Checks if the signal was ever true at any point within the last [within] window.
 *
 * Detects transient true events.
 * Translates to [ConditionType.WasEverTrue].
 *
 * @param within time window to look back for a true event
 */
fun StateFlow<Boolean>.wasEverTrueWithin(within: Duration): MonitoredCondition =
    MonitoredCondition.Leaf(this, ConditionType.WasEverTrue(within))

/**
 * Checks if the signal has flipped (true→false or false→true)
 * at least once within the last [within] window.
 *
 * Useful to detect noisy or bouncing signals.
 * Translates to [ConditionType.HasFluctuated].
 *
 * @param within time window to inspect for any transition
 */
fun StateFlow<Boolean>.hasFluctuatedWithin(within: Duration): MonitoredCondition =
    MonitoredCondition.Leaf(this, ConditionType.HasFluctuated(within))

/**
 * Logical AND of multiple monitored conditions.
 * All must evaluate to true for the combined condition to be true.
 */
fun allOf(vararg parts: MonitoredCondition): MonitoredCondition =
    MonitoredCondition.AllOf(parts.toList())

/**
 * Logical OR of multiple monitored conditions.
 * True if any single part evaluates to true.
 */
fun anyOf(vararg parts: MonitoredCondition): MonitoredCondition =
    MonitoredCondition.AnyOf(parts.toList())

/**
 * Logical NOT of a monitored condition.
 * True if [part] evaluates to false.
 */
fun not(part: MonitoredCondition): MonitoredCondition =
    MonitoredCondition.Not(part)