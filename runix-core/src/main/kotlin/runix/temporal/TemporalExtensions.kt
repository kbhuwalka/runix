package runix.temporal

import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

// Boolean temporal expressions

fun StateFlow<Boolean>.persistedFor(duration: Duration, key: String): () -> ConditionEval {
    val tracker: BooleanTracker = TemporalEngine.trackBoolean(this, key)
    return {
        tracker.evaluatePersistence(duration)
    }
}

fun StateFlow<Boolean>.occurredAtLeast(n: Int, inLast: Duration, key: String): () -> ConditionEval {
    val tracker = TemporalEngine.trackBoolean(this, key)
    return {
        val count = tracker.countInWindow(inLast)
        if (count >= n) ConditionEval.True else ConditionEval.False
    }
}

fun StateFlow<Boolean>.lastOccurredWithin(duration: Duration, key: String): () -> ConditionEval {
    val tracker = TemporalEngine.trackBoolean(this, key)
    return {
        val sinceLast = tracker.timeSinceLastTrue()
        if (sinceLast <= duration) ConditionEval.True else ConditionEval.False
    }
}

fun StateFlow<Boolean>.wasSilentFor(duration: Duration, key: String): () -> ConditionEval {
    val tracker = TemporalEngine.trackBoolean(this, key)
    return {
        val sinceLast = tracker.timeSinceLastTrue()
        if (sinceLast > duration) ConditionEval.True else ConditionEval.False
    }
}

fun StateFlow<Boolean>.debounced(duration: Duration, key: String): () -> ConditionEval {
    val tracker = TemporalEngine.trackBoolean(this, key)
    return {
        tracker.evaluateDebounce(duration)
    }
}

fun StateFlow<Boolean>.stableFor(duration: Duration, key: String): () -> ConditionEval {
    val tracker = TemporalEngine.trackBoolean(this, key)
    return {
        tracker.evaluateStability(duration)
    }
}

fun StateFlow<Boolean>.changedWithin(duration: Duration, key: String): () -> ConditionEval {
    val tracker = TemporalEngine.trackBoolean(this, key)
    return {
        if (tracker.timeSinceChange() <= duration) ConditionEval.True else ConditionEval.False
    }
}

fun StateFlow<Boolean>.notPersistedBeyond(duration: Duration, key: String): () -> ConditionEval {
    val tracker = TemporalEngine.trackBoolean(this, key)
    return {
        if (tracker.evaluatePersistence(duration) == ConditionEval.True) {
            ConditionEval.False
        } else {
            ConditionEval.True
        }
    }
}

fun StateFlow<Boolean>.cooldown(duration: Duration, key: String): () -> ConditionEval {
    val tracker = TemporalEngine.trackBoolean(this, key)
    return {
        tracker.evaluateCooldown(duration)
    }
}
