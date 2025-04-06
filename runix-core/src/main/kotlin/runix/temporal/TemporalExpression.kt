package runix.temporal

import kotlin.time.TimeSource

typealias TemporalExpression = () -> ConditionEval

fun (() -> Boolean).asTemporal(): TemporalExpression = {
    if (this()) ConditionEval.True else ConditionEval.False
}

operator fun TemporalExpression.not(): TemporalExpression = {
    when (val result = this()) {
        is ConditionEval.True -> ConditionEval.False
        is ConditionEval.False -> ConditionEval.True
        is ConditionEval.Delayed -> result
    }
}

infix fun TemporalExpression.and(other: TemporalExpression): TemporalExpression = {
    val r1 = this()
    val r2 = other()

    when {
        r1 is ConditionEval.False || r2 is ConditionEval.False -> ConditionEval.False
        r1 is ConditionEval.True && r2 is ConditionEval.True -> ConditionEval.True
        r1 is ConditionEval.Delayed && r2 is ConditionEval.Delayed ->
            ConditionEval.Delayed(minOf(r1.nextCheckAt, r2.nextCheckAt))
        r1 is ConditionEval.Delayed -> r1
        r2 is ConditionEval.Delayed -> r2
        else -> ConditionEval.False // or error: shouldn't reach this
    }
}

infix fun TemporalExpression.or(other: TemporalExpression): TemporalExpression = {
    val r1 = this()
    val r2 = other()

    when {
        r1 is ConditionEval.True || r2 is ConditionEval.True -> ConditionEval.True
        r1 is ConditionEval.False && r2 is ConditionEval.False -> ConditionEval.False
        r1 is ConditionEval.Delayed && r2 is ConditionEval.Delayed ->
            ConditionEval.Delayed(minOf(r1.nextCheckAt, r2.nextCheckAt))
        r1 is ConditionEval.Delayed -> r1
        r2 is ConditionEval.Delayed -> r2
        else -> ConditionEval.False
    }
}

fun allOf(vararg expressions: TemporalExpression): TemporalExpression {
    return {
        var delayed: ConditionEval.Delayed? = null
        for (expr in expressions) {
            when (val result = expr()) {
                is ConditionEval.False -> ConditionEval.False
                is ConditionEval.Delayed -> delayed = mergeDelays(delayed, result)
                is ConditionEval.True -> { /* no-op */ }
            }
        }
        delayed ?: ConditionEval.True
    }
}

fun anyOf(vararg expressions: TemporalExpression): TemporalExpression {
    return {
        var delayed: ConditionEval.Delayed? = null
        for (expr in expressions) {
            when (val result = expr()) {
                is ConditionEval.True -> ConditionEval.True
                is ConditionEval.Delayed -> delayed = mergeDelays(delayed, result)
                is ConditionEval.False -> { /* no-op */ }
            }
        }
        delayed ?: ConditionEval.False
    }
}

fun noneOf(vararg expressions: TemporalExpression): TemporalExpression {
    return {
        var delayed: ConditionEval.Delayed? = null
        for (expr in expressions) {
            when (val result = expr()) {
                is ConditionEval.True -> ConditionEval.False
                is ConditionEval.Delayed -> delayed = mergeDelays(delayed, result)
                is ConditionEval.False -> { /* no-op */ }
            }
        }
        delayed ?: ConditionEval.True
    }
}

private fun mergeDelays(a: ConditionEval.Delayed?, b: ConditionEval.Delayed): ConditionEval.Delayed {
    return when (a) {
        null -> b
        else -> ConditionEval.Delayed(minOf(a.nextCheckAt, b.nextCheckAt))
    }
}

private fun minOf(t1: TimeSource.Monotonic.ValueTimeMark, t2: TimeSource.Monotonic.ValueTimeMark): TimeSource.Monotonic.ValueTimeMark {
    return if (t1.hasNotPassedNow()) t1 else t2
}
