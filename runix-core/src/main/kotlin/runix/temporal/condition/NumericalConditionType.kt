package runix.temporal.condition

import runix.temporal.trackers.NumericTracker
import runix.temporal.trackers.TrackerProvider
import kotlin.time.Duration

/**
 * Internal sealed hierarchy of declarative numeric signal conditions.
 *
 * These conditions express threshold comparisons, persistence, trends,
 * and stability for `StateFlow<Double>` signals.
 *
 * Each subtype defines its own required retention window.
 */
internal sealed interface NumericConditionType : ConditionType

/**
 * Condition is true if the current value is above [threshold].
 */
internal data class IsAbove(
    val threshold: Double,
    override val retention: Duration = Duration.ZERO
) : NumericConditionType {
    override fun compileExpression(key: String, provider: TrackerProvider): TemporalExpression {
        val tracker: NumericTracker = provider.getNumericTracker(key)
        return { tracker.evaluateLatestPersisted({ it > threshold }, retention) }
    }
}

/**
 * Condition is true if the current value is below [threshold].
 */
internal data class IsBelow(
    val threshold: Double,
    override val retention: Duration = Duration.ZERO
) : NumericConditionType {
    override fun compileExpression(key: String, provider: TrackerProvider): TemporalExpression {
        val tracker = provider.getNumericTracker(key)
        return { tracker.evaluateLatestPersisted({ it < threshold }, retention) }
    }
}

/**
 * Condition is true if the value remained above [threshold] for [retention].
 */
internal data class HasPersistedAbove(
    val threshold: Double,
    override val retention: Duration
) : NumericConditionType {
    override fun compileExpression(key: String, provider: TrackerProvider): TemporalExpression {
        val tracker = provider.getNumericTracker(key)
        return { tracker.evaluateLatestPersisted({ it > threshold }, retention) }
    }
}

/**
 * Condition is true if the value remained below [threshold] for [retention].
 */
internal data class HasPersistedBelow(
    val threshold: Double,
    override val retention: Duration
) : NumericConditionType {
    override fun compileExpression(key: String, provider: TrackerProvider): TemporalExpression {
        val tracker = provider.getNumericTracker(key)
        return { tracker.evaluateLatestPersisted({ it < threshold }, retention) }
    }
}

/**
 * Condition is true if the value was ever above [threshold] during [retention].
 */
internal data class WasEverAbove(
    val threshold: Double,
    override val retention: Duration
) : NumericConditionType {
    override fun compileExpression(key: String, provider: TrackerProvider): TemporalExpression {
        val tracker = provider.getNumericTracker(key)
        return { tracker.evaluatePastTransition({ it > threshold }, Duration.ZERO) }
    }
}

/**
 * Condition is true if the value was ever below [threshold] during [retention].
 */
internal data class WasEverBelow(
    val threshold: Double,
    override val retention: Duration
) : NumericConditionType {
    override fun compileExpression(key: String, provider: TrackerProvider): TemporalExpression {
        val tracker = provider.getNumericTracker(key)
        return { tracker.evaluatePastTransition({ it < threshold }, Duration.ZERO) }
    }
}

/**
 * Condition is true if the value was above [threshold]
 * for at least [forDuration] within the last [inLast] window.
 */
internal data class WasAboveFor(
    val threshold: Double,
    val forDuration: Duration,
    val inLast: Duration
) : NumericConditionType {

    override val retention = inLast

    override fun compileExpression(key: String, provider: TrackerProvider): TemporalExpression {
        val tracker = provider.getNumericTracker(key)
        return { tracker.evaluatePastTransition({ it > threshold }, forDuration) }
    }
}

/**
 * Condition is true if the value was below [threshold]
 * for at least [forDuration] within the last [inLast] window.
 */
internal data class WasBelowFor(
    val threshold: Double,
    val forDuration: Duration,
    val inLast: Duration
) : NumericConditionType {

    override val retention = inLast

    override fun compileExpression(key: String, provider: TrackerProvider): TemporalExpression {
        val tracker = provider.getNumericTracker(key)
        return { tracker.evaluatePastTransition({ it < threshold }, forDuration) }
    }
}

/**
 * Condition is true if the signal was increasing (monotonically)
 * for at least the duration defined by [retention].
 */
internal data class Increasing(
    override val retention: Duration
) : NumericConditionType {
    override fun compileExpression(key: String, provider: TrackerProvider): TemporalExpression {
        val tracker = provider.getNumericTracker(key)
        return { tracker.evaluateTrend { curr, prev -> curr < prev } }
    }
}

/**
 * Condition is true if the signal was decreasing (monotonically)
 * for at least the duration defined by [retention].
 */
internal data class Decreasing(
    override val retention: Duration
) : NumericConditionType {
    override fun compileExpression(key: String, provider: TrackerProvider): TemporalExpression {
        val tracker = provider.getNumericTracker(key)
        return { tracker.evaluateTrend { curr, prev -> curr > prev } }
    }
}

/**
 * Condition is true if the signal remained stable within [margin]
 * for the entire [retention] duration.
 *
 * This means that the spread (max - min) of values never exceeded 2 × [margin].
 */
internal data class StableWithin(
    val margin: Double,
    override val retention: Duration
) : NumericConditionType {

    override fun compileExpression(key: String, provider: TrackerProvider): TemporalExpression {
        val tracker = provider.getNumericTracker(key)
        return {
            tracker.evaluateStabilityWindow { spread -> spread > margin * 2 }
        }
    }
}

/**
 * Condition is true if any fluctuation in value exceeded [margin]
 * at any point within the last [retention] window.
 *
 * This detects spike-like behavior or volatility breaches.
 */
internal data class FluctuatedBeyond(
    val margin: Double,
    override val retention: Duration
) : NumericConditionType {

    override fun compileExpression(key: String, provider: TrackerProvider): TemporalExpression {
        val tracker = provider.getNumericTracker(key)
        return {
            tracker.hasFluctuatedBeyond(margin)
        }
    }
}