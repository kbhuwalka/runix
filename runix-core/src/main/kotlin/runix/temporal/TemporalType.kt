package runix.temporal

import kotlin.time.Duration

sealed class TemporalType {
    abstract fun compileEvaluation(key: String): TemporalExpression
    abstract fun retentionWindow(): Duration

    object Instant : TemporalType() {
        override fun compileEvaluation(key: String): TemporalExpression = {
            val tracker = TemporalEngine.getBooleanTracker(key)
            tracker.isTrue()
        }

        override fun retentionWindow(): Duration = Duration.ZERO
    }

    data class Persisted(val duration: Duration) : TemporalType() {
        override fun compileEvaluation(key: String): TemporalExpression = {
            val tracker = TemporalEngine.getBooleanTracker(key)
            tracker.hasPersistedFor(duration)
        }

        override fun retentionWindow(): Duration = duration
    }

    data class WasStable(val duration: Duration) : TemporalType() {
        override fun compileEvaluation(key: String): TemporalExpression = {
            val tracker = TemporalEngine.getBooleanTracker(key)
            tracker.hasBeenStableFor(duration)
        }

        override fun retentionWindow(): Duration = duration
    }
}
