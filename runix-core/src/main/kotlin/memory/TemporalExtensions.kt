package runix.memory

import kotlinx.coroutines.flow.Flow
import runix.monitor.TemporalEngine
import kotlin.time.Duration

fun Flow<Boolean>.persistsFor(duration: Duration, key: String): TemporalExpression {
    val tracker = TemporalEngine.trackBoolean(this, key)
    return { tracker.getPersistedDuration() > duration }
}

fun Flow<Boolean>.occurredAtLeast(n: Int, inLast: Duration, key: String): TemporalExpression {
    val tracker = TemporalEngine.trackBoolean(this, key)
    return { tracker.countInWindow(inLast) >= n }
}

fun Flow<Boolean>.lastOccurredWithin(duration: Duration, key: String): TemporalExpression {
    val tracker = TemporalEngine.trackBoolean(this, key)
    return { tracker.timeSinceLastTrue() <= duration }
}

fun Flow<Boolean>.wasSilentFor(duration: Duration, key: String): TemporalExpression {
    val tracker = TemporalEngine.trackBoolean(this, key)
    return { tracker.timeSinceLastTrue() > duration }
}

fun Flow<Double>.averageOver(window: Duration, threshold: Double, key: String): TemporalExpression {
    val tracker = TemporalEngine.trackNumeric(this, key)
    return { tracker.average(window) > threshold }
}

fun Flow<Double>.varianceOver(window: Duration, threshold: Double, key: String): TemporalExpression {
    val tracker = TemporalEngine.trackNumeric(this, key)
    return { tracker.variance(window) > threshold }
}