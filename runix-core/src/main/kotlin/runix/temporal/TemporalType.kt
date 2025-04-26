package runix.temporal

import kotlin.time.Duration

sealed class TemporalType {
    abstract fun compileEvaluation(key: String): TemporalExpression
    abstract fun retentionWindow(): Duration

    object Instant : TemporalType() {
        override fun compileEvaluation(key: String): TemporalExpression = {
            val tracker = TemporalEngine.getBooleanTracker(key)
            tracker.compileWhenTrue()()
        }

        override fun retentionWindow(): Duration = Duration.ZERO
    }

    data class Persisted(val duration: Duration) : TemporalType() {
        override fun compileEvaluation(key: String): TemporalExpression = {
            val tracker = TemporalEngine.getBooleanTracker(key)
            tracker.compilePersistedFor(duration)()
        }

        override fun retentionWindow(): Duration = duration
    }

    data class WasStable(val duration: Duration) : TemporalType() {
        override fun compileEvaluation(key: String): TemporalExpression = {
            val tracker = TemporalEngine.getBooleanTracker(key)
            tracker.compileWasStableFor(duration)()
        }

        override fun retentionWindow(): Duration = duration
    }
}
