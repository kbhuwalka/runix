package runix.temporal

import kotlin.time.Duration
import kotlin.time.TimeSource

fun BooleanTracker.compilePersistedFor(duration: Duration): TemporalExpression {
    return {
        val elapsed = currentStartMark?.elapsedNow()

        when {
            !lastValue || elapsed == null -> ConditionEval.False
            elapsed >= duration -> ConditionEval.True
            else -> ConditionEval.Delayed(
                TimeSource.Monotonic.markNow().plus(duration - elapsed)
            )
        }
    }
}

fun BooleanTracker.compileWasStableFor(duration: Duration): TemporalExpression {
    return {
        val elapsed = stableSince.elapsedNow()
        if (elapsed >= duration) {
            ConditionEval.True
        } else {
            ConditionEval.Delayed(stableSince.plus(duration - elapsed))
        }
    }
}

fun BooleanTracker.compileWhenTrue(): TemporalExpression {
    return {
        if (lastValue) ConditionEval.True else ConditionEval.False
    }
}
