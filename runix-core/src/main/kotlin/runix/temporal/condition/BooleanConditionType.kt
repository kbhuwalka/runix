package runix.temporal.condition

import runix.temporal.trackers.BooleanTracker
import runix.temporal.trackers.TrackerProvider
import kotlin.time.Duration

/**
 * Internal sealed hierarchy of declarative Boolean condition types.
 *
 * Each subtype defines a specific logical behavior (e.g. isTrue, hasPersistedTrue)
 * and owns its own required retention window.
 *
 * These types compile themselves into a [TemporalExpression] by:
 * - Using the supplied [key] (unique signal identity for this condition)
 * - Asking the [TrackerProvider] to retrieve or create a tracker for that key and retention
 *
 * DSLs map developer intent into these types behind the scenes.
 */
internal sealed interface BooleanConditionType : ConditionType

/**
 * Condition is true if the signal is currently `true`.
 * This is a snapshot evaluation.
 */
internal data class IsTrue(
    override val retention: Duration = Duration.ZERO
) : BooleanConditionType {
    override fun compileExpression(key: String, provider: TrackerProvider): TemporalExpression {
        val tracker: BooleanTracker = provider.getBooleanTracker(key)
        return { tracker.evaluateLatestPersisted({ it }, retention) }
    }
}

/**
 * Condition is true if the signal has remained `true` continuously
 * for at least the given [retention] duration.
 */
internal data class HasPersistedTrue(
    override val retention: Duration
) : BooleanConditionType {
    override fun compileExpression(key: String, provider: TrackerProvider): TemporalExpression {
        val tracker = provider.getBooleanTracker(key)
        return { tracker.evaluateLatestPersisted({ it }, retention) }
    }
}

/**
 * Condition is true if the signal was `true` for at least [forDuration]
 * at any point within the last [inLast] window.
 */
internal data class WasTrueFor(
    val forDuration: Duration,
    val inLast: Duration
) : BooleanConditionType {

    override val retention = inLast

    override fun compileExpression(key: String, provider: TrackerProvider): TemporalExpression {
        val tracker = provider.getBooleanTracker(key)
        return { tracker.evaluatePastTransition({ it }, forDuration) }
    }
}

/**
 * Condition is true if the signal has ever been `true` at all
 * within the current memory window.
 */
internal data class WasEverTrue(
    val inLast: Duration
) : BooleanConditionType {
    override val retention = inLast

    override fun compileExpression(key: String, provider: TrackerProvider): TemporalExpression {
        val tracker = provider.getBooleanTracker(key)
        return { tracker.evaluatePastTransition({ it }, Duration.ZERO) }
    }
}

/**
 * Condition is true if the signal changed value (true ↔ false)
 * at least once during the retention window.
 */
internal data class HasFluctuated(
    val inLast: Duration
) : BooleanConditionType {
    override val retention = inLast

    override fun compileExpression(key: String, provider: TrackerProvider): TemporalExpression {
        val tracker = provider.getBooleanTracker(key)
        return { tracker.evaluateFluctuated() }
    }
}