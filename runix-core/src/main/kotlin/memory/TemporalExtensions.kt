package runix.memory

import kotlinx.coroutines.flow.Flow
import runix.core.logging.memory.BooleanTracker
import runix.core.logging.memory.NumericTracker
import runix.monitor.TemporalEngine
import kotlin.time.Duration

// Boolean temporal expressions

fun Flow<Boolean>.persistedFor(duration: Duration, key: String): () -> ConditionEval {
    val tracker: BooleanTracker = TemporalEngine.trackBoolean(this, key)
    return {
        tracker.evaluatePersistence(duration)
    }
}

fun Flow<Boolean>.occurredAtLeast(n: Int, inLast: Duration, key: String): () -> ConditionEval {
    val tracker = TemporalEngine.trackBoolean(this, key)
    return {
        val count = tracker.countInWindow(inLast)
        if (count >= n) ConditionEval.True else ConditionEval.False
    }
}

fun Flow<Boolean>.lastOccurredWithin(duration: Duration, key: String): () -> ConditionEval {
    val tracker = TemporalEngine.trackBoolean(this, key)
    return {
        val sinceLast = tracker.timeSinceLastTrue()
        if (sinceLast <= duration) ConditionEval.True else ConditionEval.False
    }
}

fun Flow<Boolean>.wasSilentFor(duration: Duration, key: String): () -> ConditionEval {
    val tracker = TemporalEngine.trackBoolean(this, key)
    return {
        val sinceLast = tracker.timeSinceLastTrue()
        if (sinceLast > duration) ConditionEval.True else ConditionEval.False
    }
}

// Numeric expressions

fun Flow<Double>.averageOver(window: Duration, threshold: Double, key: String): () -> ConditionEval {
    val tracker: NumericTracker = TemporalEngine.trackNumeric(this, key)
    return {
        val avg = tracker.average(window)
        if (avg > threshold) ConditionEval.True else ConditionEval.False
    }
}

fun Flow<Double>.varianceOver(window: Duration, threshold: Double, key: String): () -> ConditionEval {
    val tracker: NumericTracker = TemporalEngine.trackNumeric(this, key)
    return {
        val variance = tracker.variance(window)
        if (variance > threshold) ConditionEval.True else ConditionEval.False
    }
}