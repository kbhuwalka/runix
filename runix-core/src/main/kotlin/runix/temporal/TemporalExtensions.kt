package runix.temporal

import kotlinx.coroutines.flow.StateFlow
import runix.internal.MonitorThrottleRegistry
import runix.internal.ThrottleResult
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

// Boolean temporal expressions

/**
 * True if the signal has been true continuously for the full duration.
 */
fun StateFlow<Boolean>.persistedFor(duration: Duration, key: String): TemporalExpression {
    val tracker = TemporalEngine.trackBoolean(this, key)
    return { tracker.evaluatePersistence(duration) }
}

/**
 * True if the signal has flipped to true at least [n] times in the given time window.
 */
fun StateFlow<Boolean>.occurredAtLeast(n: Int, inLast: Duration, key: String): TemporalExpression {
    val tracker = TemporalEngine.trackBoolean(this, key)
    return {
        val count = tracker.countInWindow(inLast)
        if (count >= n) ConditionEval.True else ConditionEval.False
    }
}

/**
 * True if the signal last became true within [duration].
 */
fun StateFlow<Boolean>.lastTrueWasWithin(duration: Duration, key: String): TemporalExpression {
    val tracker = TemporalEngine.trackBoolean(this, key)
    return {
        val sinceLast = tracker.timeSinceLastTrue()
        if (sinceLast <= duration) ConditionEval.True else ConditionEval.False
    }
}

/**
 * True if the signal has NOT been true within the last [duration].
 */
fun StateFlow<Boolean>.wasSilentFor(duration: Duration, key: String): TemporalExpression {
    val tracker = TemporalEngine.trackBoolean(this, key)

    return {
        // Signal is currently true → not silent
        // Guard clause: signal has never been true, no meaningful silence
        if (this.value || tracker.lastTrueMark == null) {
            ConditionEval.False
        } else {
            val silenceSince = tracker.stableSince
            val elapsed = silenceSince.elapsedNow()
            if (elapsed > duration) {
                ConditionEval.True
            } else {
                val delayUntil = silenceSince.plus(duration-elapsed)
                ConditionEval.Delayed(delayUntil)
            }
        }
    }
}

/**
 * True if the signal has remained unchanged for [duration].
 */
fun StateFlow<Boolean>.wasStableFor(duration: Duration, key: String): TemporalExpression {
    val tracker = TemporalEngine.trackBoolean(this, key)
    return { tracker.evaluateStability(duration) }
}

/**
 * True if the signal has changed within the last [duration].
 */
fun StateFlow<Boolean>.changedWithin(duration: Duration, key: String): TemporalExpression {
    val tracker = TemporalEngine.trackBoolean(this, key)
    return {
        if (tracker.timeSinceChange() <= duration) {
            ConditionEval.True
        } else {
            ConditionEval.False
        }
    }
}

/**
 * True if the signal has NOT persisted for the given duration.
 */
fun StateFlow<Boolean>.notPersistedFor(duration: Duration, key: String): TemporalExpression {
    val tracker = TemporalEngine.trackBoolean(this, key)
    return {
        when (tracker.evaluatePersistence(duration)) {
            ConditionEval.True -> ConditionEval.False
            else -> ConditionEval.True
        }
    }
}

/**
 * Converts a Boolean StateFlow into a traceable, reactive temporal condition.
 * This expression returns TRUE when the current value is true,
 * and FALSE otherwise. It registers with TemporalEngine for traceability.
 */
fun StateFlow<Boolean>.asCondition(key: String): TemporalExpression {
    TemporalEngine.trackBoolean(this, key)
    return {
        if (this.value) ConditionEval.True else ConditionEval.False
    }
}