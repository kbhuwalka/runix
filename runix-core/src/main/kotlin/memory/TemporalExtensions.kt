package runix.memory

import kotlinx.coroutines.flow.Flow
import runix.monitor.TemporalEngine
import kotlin.time.Duration

class TemporalExtensions {
}

fun Flow<Boolean>.persistsFor(duration: Duration): TemporalExpression {
    val tracker = TemporalEngine.trackBoolean(this)
    return { tracker.getPersistedDuration() > duration }
}

fun Flow<Boolean>.occurredAtLeast(n: Int, inLast: Duration): TemporalExpression {
    val tracker = TemporalEngine.trackBoolean(this)
    return { tracker.countInWindow(inLast) >= n }
}

fun Flow<Boolean>.lastOccurredWithin(duration: Duration): TemporalExpression {
    val tracker = TemporalEngine.trackBoolean(this)
    return { tracker.timeSinceLastTrue() <= duration }
}

fun Flow<Boolean>.wasSilentFor(duration: Duration): TemporalExpression {
    val tracker = TemporalEngine.trackBoolean(this)
    return { tracker.timeSinceLastTrue() > duration }
}

fun Flow<Double>.averageOver(window: Duration, threshold: Double): TemporalExpression {
    val tracker = TemporalEngine.trackNumeric(this)
    return { tracker.average(window) > threshold }
}

fun Flow<Double>.varianceOver(window: Duration, threshold: Double): TemporalExpression {
    val tracker = TemporalEngine.trackNumeric(this)
    return { tracker.variance(window) > threshold }
}