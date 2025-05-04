package runix.temporal.trackers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import runix.runtime.internal.RuntimeScope
import runix.temporal.condition.ConditionEval
import runix.temporal.time.Time
import runix.temporal.time.durationSince
import kotlin.time.Duration

/**
 * Tracks a numeric (Double) signal over time and enables declarative,
 * time-aware cognitive evaluations such as thresholds, trends, and stability.
 */
internal open class NumericTracker(
    flow: StateFlow<Double>,
    private val retention: Duration,
    scope: CoroutineScope = RuntimeScope.scope,
    onUpdate: () -> Unit
) : BaseTracker<Double>(flow, retention, scope, onUpdate) {

    override fun registerWith(key: String) {
        TrackerRegistry.register(key, this)
    }

    /**
     * Evaluates whether the most recent signal value satisfies [match]
     * and has remained continuously true for at least [forTime].
     *
     * - Returns [ConditionEval.True] if the current value matches and dwell-time is satisfied.
     * - Returns [ConditionEval.Delayed] if the value matches but dwell-time is not yet satisfied.
     * - Returns [ConditionEval.False] if the current value does not match.
     *
     * This is a persistence check based on the most recent value — no history scan is performed.
     * The duration is measured since the last value change (as tracked in history).
     *
     * Commonly used for threshold-based dwell checks like:
     * "value > 50 for 10s" or "value within ±1.0 for 5s".
     *
     * @param match predicate to evaluate the latest value
     * @param forTime required continuous duration the predicate must hold
     */
    internal fun evaluateLatestPersisted(
        match: (Double) -> Boolean,
        forTime: Duration
    ): ConditionEval {
        val now = Time.markNow()
        history.prune(now)

        val last = history.last()
        if (!match(last.value)) {
            return ConditionEval.False
        }

        val elapsed = now.durationSince(last.timestamp)
        return if (elapsed >= forTime) {
            ConditionEval.True
        } else {
            ConditionEval.Delayed(last.timestamp.plus(forTime))
        }
    }

    /**
     * Evaluates whether any value in the retention window satisfied [match]
     * for a continuous span of at least [forTime].
     *
     * - Returns [ConditionEval.True] if any such dwell period is found.
     * - Returns [ConditionEval.Delayed] if the most recent value matches but dwell time is not yet satisfied.
     * - Returns [ConditionEval.False] if no qualifying span is found.
     *
     * This is a time-bounded historical persistence check: it scans the signal history
     * for intervals where the value matched and remained stable for long enough.
     *
     * Commonly used for:
     * - "Was value > 50 for 10s at any point?"
     * - "Did the signal remain inside 0..1 range for at least 5s?"
     *
     * @param match predicate applied to each value in history
     * @param forTime the required duration that match must hold to succeed
     */
    internal fun evaluatePastTransition(
        match: (Double) -> Boolean,
        forTime: Duration
    ): ConditionEval {
        val now = Time.markNow()
        history.prune(now)

        val iterator = history.entries().iterator()
        if (!iterator.hasNext()) return ConditionEval.False

        var prev = iterator.next()
        while (iterator.hasNext()) {
            val curr = iterator.next()

            val dwellDuration = curr.timestamp.durationSince(prev.timestamp)
            if (match(prev.value) && dwellDuration >= forTime) {
                return ConditionEval.True
            }

            prev = curr
        }

        if (!match(prev.value)) {
            return ConditionEval.False
        }

        val elapsed = now - prev.timestamp
        return if (elapsed >= forTime) {
            ConditionEval.True
        } else {
            ConditionEval.Delayed(prev.timestamp.plus(forTime))
        }
    }

    /**
     * Evaluates whether the signal has maintained a consistent monotonic trend
     * (either non-decreasing or non-increasing) across the current retention window.
     *
     * - Returns [ConditionEval.True] if a contiguous monotonic sequence satisfying the trend
     *   spans at least the full retention window.
     * - Returns [ConditionEval.Delayed] if more time is needed to accumulate a full satisfying span.
     * - Resets the evaluation window dynamically when [violatesTrend] returns true,
     *   allowing trend recovery and re-evaluation from new points.
     *
     * Used for detecting behaviors like sustained increasing or decreasing trends.
     *
     * @param violatesTrend a predicate that determines when a trend violation occurs between consecutive values
     */
    internal inline fun evaluateTrend(
        crossinline violatesTrend: (Double, Double) -> Boolean
    ): ConditionEval {
        val now = Time.markNow()
        history.prune(now)

        val iterator = history.entries().iterator()
        if (!iterator.hasNext()) return ConditionEval.False

        var first = iterator.next()
        var previousValue = first.value
        var last = first

        while (iterator.hasNext()) {
            val current = iterator.next()

            if (violatesTrend(current.value, previousValue)) {
                first = current
            }
            last = current
            previousValue = current.value

            val span = last.timestamp - first.timestamp
            if (span >= retention) {
                return ConditionEval.True
            }
        }

        val finalSpan = now - first.timestamp
        return if (finalSpan >= retention) {
            ConditionEval.True
        } else {
            ConditionEval.Delayed(first.timestamp.plus(retention))
        }
    }

    /**
     * Evaluates whether the signal has remained stable within a desired margin
     * across the entire retention window.
     *
     * This function incrementally evaluates signal spread (max - min) and resets
     * the tracking window if [shouldReset] returns true based on the observed spread.
     *
     * - Returns [ConditionEval.True] if a contiguous stable window spans the full retention window.
     * - Returns [ConditionEval.Delayed] if more time is needed.
     * - Resets the window whenever [shouldReset] is violated.
     *
     * Used for modeling:
     * - `isStableWithin(...)`: sustained bounded spread
     * - `hasFluctuatedBeyond(...)`: transient margin violations
     *
     * @param shouldReset Predicate to determine if spread exceeds the allowable margin
     */
    internal inline fun evaluateStabilityWindow(
        crossinline shouldReset: (Double) -> Boolean
    ): ConditionEval {
        val now = Time.markNow()
        history.prune(now)

        val iterator = history.entries().iterator()
        if (!iterator.hasNext()) return ConditionEval.False

        val first = iterator.next()
        var firstTimestamp = first.timestamp
        var lastTimestamp = first.timestamp
        var minValue = first.value
        var maxValue = first.value

        while (iterator.hasNext()) {
            val current = iterator.next()

            minValue = minOf(minValue, current.value)
            maxValue = maxOf(maxValue, current.value)
            lastTimestamp = current.timestamp

            val spread = maxValue - minValue
            val span = lastTimestamp - firstTimestamp

            if (shouldReset(spread)) {
                // Reset the stability window
                firstTimestamp = current.timestamp
                minValue = current.value
                maxValue = current.value
            } else if (span >= retention) {
                return ConditionEval.True
            }
        }

        val finalSpan = now - firstTimestamp
        return if (finalSpan >= retention) {
            ConditionEval.True
        } else {
            ConditionEval.Delayed(firstTimestamp.plus(retention))
        }
    }

    /**
     * Evaluates whether the signal has fluctuated beyond the given [margin]
     * at any point within the current retention window.
     *
     * This check compares the min and max values across the retained history
     * and returns [ConditionEval.True] as soon as the spread (max - min) exceeds `margin * 2`.
     *
     * Unlike stability checks, this method does not require the fluctuation to hold for a duration —
     * it triggers immediately upon detection of any volatility beyond the allowed threshold.
     *
     * - Returns [ConditionEval.True] if spread exceeds margin × 2 at any point
     * - Returns [ConditionEval.False] if values are stable or history is insufficient
     *
     * @param margin The max deviation allowed from center before the signal is considered unstable
     */
    internal fun hasFluctuatedBeyond(margin: Double): ConditionEval {
        val now = Time.markNow()
        history.prune(now)

        val values = history.entries().map { it.value }
        if (values.size < 2) return ConditionEval.False

        val spread = values.max() - values.min()
        return if (spread > margin * 2) {
            ConditionEval.True
        } else {
            ConditionEval.False
        }
    }
}
