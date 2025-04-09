package runix.temporal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * Tracks changes to a StateFlow<Boolean> signal and evaluates time-aware conditions over time.
 *
 * This tracker assumes hot signal input and stores only changes (true/false flips) over time.
 * History is automatically pruned based on the longest active evaluation window requested,
 * ensuring memory usage is bounded while preserving correctness.
 *
 * Evaluators include:
 * - debounce: suppress flicker
 * - stability: detect no change
 * - cooldown: suppress re-triggers
 */
class BooleanTracker(flow: StateFlow<Boolean>) {
    data class ValueWithMark(val value: Boolean, val timestamp: TimeSource.Monotonic.ValueTimeMark)

    private val timestamps = mutableListOf<TimeSource.Monotonic.ValueTimeMark>()
    private var currentStartMark: TimeSource.Monotonic.ValueTimeMark? = null
    private var lastTrueMark: TimeSource.Monotonic.ValueTimeMark? = null
    private val clock = TimeSource.Monotonic
    private val valueHistory = mutableListOf<ValueWithMark>()
    private var lastValue: Boolean? = null
    private var maxRequiredDuration: Duration = Duration.ZERO
    private var stableSince: TimeSource.Monotonic.ValueTimeMark? = null

    init {
        CoroutineScope(Dispatchers.Default).launch {
            flow.collect { value ->
                val now = clock.markNow()
                if (lastValue == null || lastValue != value) {
                    valueHistory.add(ValueWithMark(value, now))
                    stableSince = now
                }
                lastValue = value
                if (value) {
                    if (currentStartMark == null) {
                        currentStartMark = clock.markNow()
                    }
                    lastTrueMark = clock.markNow()
                    timestamps.add(clock.markNow())
                } else {
                    currentStartMark = null
                }
            }
        }
    }

    private fun pruneHistory(duration: Duration) {
        if (duration > maxRequiredDuration) {
            maxRequiredDuration = duration
        }
        valueHistory.removeIf { it.timestamp.elapsedNow() > maxRequiredDuration }
    }

    fun evaluatePersistence(duration: Duration): ConditionEval {
        val persisted = currentStartMark?.elapsedNow() ?: return ConditionEval.False
        return if (persisted >= duration) {
            ConditionEval.True
        } else {
            val nextCheckAt = clock.markNow().plus(duration - persisted)
            ConditionEval.Delayed(nextCheckAt)
        }
    }

    fun countInWindow(window: Duration): Int {
        timestamps.removeIf { it.elapsedNow() > window }
        return timestamps.count()
    }

    fun timeSinceLastTrue(): Duration {
        return lastTrueMark?.elapsedNow() ?: Duration.INFINITE
    }

    fun evaluateStability(duration: Duration): ConditionEval {
        pruneHistory(duration)

        val since = stableSince ?: return ConditionEval.False
        val elapsed = since.elapsedNow()

        return if (elapsed >= duration) {
            ConditionEval.True
        } else {
            ConditionEval.Delayed(since.plus(duration - elapsed))
        }
    }

    fun timeSinceChange(): Duration {
        val lastChange = valueHistory.lastOrNull()?.timestamp ?: return Duration.INFINITE
        return lastChange.elapsedNow()
    }
}
