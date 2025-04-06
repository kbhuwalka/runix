package runix.temporal

import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

fun StateFlow<Double>.averageOver(window: Duration, threshold: Double, key: String): () -> ConditionEval {
    val tracker: NumericTracker = TemporalEngine.trackNumeric(this, key)
    return {
        val avg = tracker.average(window)
        if (avg > threshold) ConditionEval.True else ConditionEval.False
    }
}

fun StateFlow<Double>.varianceOver(window: Duration, threshold: Double, key: String): () -> ConditionEval {
    val tracker: NumericTracker = TemporalEngine.trackNumeric(this, key)
    return {
        val variance = tracker.variance(window)
        if (variance > threshold) ConditionEval.True else ConditionEval.False
    }
}

fun StateFlow<Double>.increasedBy(threshold: Double, within: Duration, key: String): () -> ConditionEval {
    val tracker = TemporalEngine.trackNumeric(this, key)
    return { tracker.evaluateIncrease(threshold, within) }
}

fun StateFlow<Double>.decreasedBy(threshold: Double, within: Duration, key: String): () -> ConditionEval {
    val tracker = TemporalEngine.trackNumeric(this, key)
    return { tracker.evaluateDecrease(threshold, within) }
}

fun StateFlow<Double>.stayedAbove(value: Double, duration: Duration, key: String): () -> ConditionEval {
    val tracker = TemporalEngine.trackNumeric(this, key)
    return { tracker.evaluateStabilityAbove(value, duration) }
}

fun StateFlow<Double>.stayedBelow(value: Double, duration: Duration, key: String): () -> ConditionEval {
    val tracker = TemporalEngine.trackNumeric(this, key)
    return { tracker.evaluateStabilityBelow(value, duration) }
}

fun StateFlow<Double>.enteredRange(min: Double, max: Double, duration: Duration, key: String): () -> ConditionEval {
    val tracker = TemporalEngine.trackNumeric(this, key)
    return { tracker.evaluateStabilityInRange(min, max, duration) }
}
