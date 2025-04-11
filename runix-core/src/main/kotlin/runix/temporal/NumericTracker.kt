package runix.temporal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import runix.internal.RunixRuntimeScope
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * Tracks a numeric signal and evaluates temporal expressions over it.
 *
 * Supports increase/decrease detection, stability checks above/below thresholds,
 * and value range persistence over time.
 */
class NumericTracker(flow: StateFlow<Double>) {
    data class ValueWithMark(val value: Double, val timestamp: TimeSource.Monotonic.ValueTimeMark)

    private val clock = TimeSource.Monotonic
    private val valueHistory = mutableListOf<ValueWithMark>()
    private var maxRequiredDuration: Duration = Duration.ZERO

    init {
        RunixRuntimeScope.scope.launch {
            flow.collect { value ->
                val now = clock.markNow()
                valueHistory.add(ValueWithMark(value, now))
            }
        }
    }

    private fun pruneHistory(duration: Duration) {
        if (duration > maxRequiredDuration) {
            maxRequiredDuration = duration
        }
        valueHistory.removeIf { it.timestamp.elapsedNow() > maxRequiredDuration }
    }

    fun average(window: Duration): Double {
        pruneHistory(window)
        val relevant = valueHistory.filter { it.timestamp.elapsedNow() <= window }.map { it.value }
        return if (relevant.isNotEmpty()) relevant.average() else 0.0
    }

    fun variance(window: Duration): Double {
        pruneHistory(window)
        val relevant = valueHistory.filter { it.timestamp.elapsedNow() <= window }.map { it.value }
        val avg = relevant.average()
        return if (relevant.isNotEmpty()) {
            relevant.map { (it - avg).pow(2) }.average()
        } else {
            0.0
        }
    }

    fun evaluateIncrease(threshold: Double, within: Duration): ConditionEval {
        pruneHistory(within)
        val reference = valueHistory.firstOrNull { it.timestamp.elapsedNow() <= within } ?: return ConditionEval.False
        val current = valueHistory.lastOrNull() ?: return ConditionEval.False
        return if (current.value - reference.value >= threshold) {
            ConditionEval.True
        } else {
            ConditionEval.False
        }
    }

    fun evaluateDecrease(threshold: Double, within: Duration): ConditionEval {
        pruneHistory(within)
        val reference = valueHistory.firstOrNull { it.timestamp.elapsedNow() <= within } ?: return ConditionEval.False
        val current = valueHistory.lastOrNull() ?: return ConditionEval.False
        return if (reference.value - current.value >= threshold) {
            ConditionEval.True
        } else {
            ConditionEval.False
        }
    }

    fun evaluateStabilityAbove(value: Double, duration: Duration): ConditionEval {
        pruneHistory(duration)
        val earliest = valueHistory.firstOrNull() ?: return ConditionEval.False
        if (earliest.timestamp.elapsedNow() < duration) {
            return ConditionEval.Delayed(earliest.timestamp.plus(duration))
        }
        return if (valueHistory.all { it.value > value }) ConditionEval.True else ConditionEval.False
    }

    fun evaluateStabilityBelow(value: Double, duration: Duration): ConditionEval {
        pruneHistory(duration)
        val earliest = valueHistory.firstOrNull() ?: return ConditionEval.False
        if (earliest.timestamp.elapsedNow() < duration) {
            return ConditionEval.Delayed(earliest.timestamp.plus(duration))
        }
        return if (valueHistory.all { it.value < value }) ConditionEval.True else ConditionEval.False
    }

    fun evaluateStabilityInRange(min: Double, max: Double, duration: Duration): ConditionEval {
        pruneHistory(duration)
        val earliest = valueHistory.firstOrNull() ?: return ConditionEval.False
        if (earliest.timestamp.elapsedNow() < duration) {
            return ConditionEval.Delayed(earliest.timestamp.plus(duration))
        }
        return if (valueHistory.all { it.value in min..max }) ConditionEval.True else ConditionEval.False
    }
}
