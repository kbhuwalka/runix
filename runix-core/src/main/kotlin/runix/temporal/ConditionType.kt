package runix.temporal

import kotlin.time.Duration

/**
 * Represents the high-level cognitive condition types
 * that can be applied to a boolean signal over time.
 *
 * Each condition defines:
 * - How it should be evaluated at runtime (via compileEvaluation()).
 * - How much history (retention window) is needed to evaluate it (via retentionWindow()).
 */
sealed class ConditionType {

    /**
     * Checks if the signal is currently true at this instant.
     * Requires no history.
     */
    object IsTrueNow : ConditionType() {
        override fun compileExpression(key: String): TemporalExpression = {
            val tracker = TemporalEngine.getBooleanTracker(key)
            tracker.isTrue()
        }

        override val retentionWindow = Duration.ZERO
    }

    /**
     * Checks if the signal has continuously persisted true for at least [duration].
     * Memoryless evaluation (depends only on the current open true period).
     *
     * @param duration The minimum persistence time required.
     */
    data class Persisted(val duration: Duration) : ConditionType() {
        override fun compileExpression(key: String): TemporalExpression = {
            val tracker = TemporalEngine.getBooleanTracker(key)
            tracker.hasPersistedFor(duration)
        }

        override val retentionWindow = Duration.ZERO
    }

    /**
     * Checks if the signal (true or false) has remained stable (unchanging) for at least [duration].
     *
     * Memoryless evaluation (depends only on last transition).
     */
    data class WasStable(val duration: Duration) : ConditionType() {
        override fun compileExpression(key: String): TemporalExpression = {
            val tracker = TemporalEngine.getBooleanTracker(key)
            tracker.hasBeenStableFor(duration)
        }

        override val retentionWindow = Duration.ZERO
    }

    /**
     * Checks if the signal was ever true inside the last [within] window.
     *
     * Requires memory of event history to detect transient true signals over time.
     */
    data class WasEverTrue(val within: Duration) : ConditionType() {
        override fun compileExpression(key: String): TemporalExpression = {
            val tracker = TemporalEngine.getBooleanTracker(key)
            tracker.wasEverTrue()
        }

        override val retentionWindow = within
    }

    /**
     * Checks if the signal fluctuated (true ↔ false) inside the last [within] window.
     *
     * Requires memory of event history to detect transitions over time.
     */
    data class HasFluctuated(val within: Duration) : ConditionType() {
        override fun compileExpression(key: String): TemporalExpression = {
            val tracker = TemporalEngine.getBooleanTracker(key)
            tracker.hasFluctuated()
        }

        override val retentionWindow = within
    }

    /**
     * Checks if the signal accumulated [target] amount of true time
     * inside the last [within] window.
     *
     * Requires memory of event history to sum true periods across window.
     */
    data class PersistedForAtLeast(val target: Duration, val within: Duration) : ConditionType() {
        override fun compileExpression(key: String): TemporalExpression = {
            val tracker = TemporalEngine.getBooleanTracker(key)
            tracker.hasPersistedForAtLeast(target)
        }

        override val retentionWindow = within
    }

    /**
     * Abstract method that compiles this declarative condition
     * into a runtime TemporalExpression given a key associated with a Tracker.
     *
     * @param key The unique identifier associated with the BooleanTracker.
     * @return The compiled TemporalExpression ready for runtime evaluation.
     */
    internal abstract fun compileExpression(key: String): TemporalExpression

    /**
     * Defines the minimum retention window required to evaluate this condition type.
     *
     * Conditions that require full signal history (e.g., accumulation, fluctuation)
     * will request longer retention windows.
     * Instantaneous or memoryless conditions will require zero or minimal retention.
     */
    internal abstract val retentionWindow: Duration
}