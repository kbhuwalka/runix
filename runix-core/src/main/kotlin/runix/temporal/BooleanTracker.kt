package runix.temporal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import robot_cleaner.logger
import runix.internal.RunixRuntimeScope
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * Tracks changes to a StateFlow<Boolean> signal and evaluates time-aware conditions over time.
 *
 * Supports persistence, stability, silence, and change tracking.
 */
class BooleanTracker(flow: StateFlow<Boolean>) {
    data class ValueWithMark(val value: Boolean, val timestamp: TimeSource.Monotonic.ValueTimeMark)

    private val clock = TimeSource.Monotonic

    private val timestamps = mutableListOf<TimeSource.Monotonic.ValueTimeMark>() // all true values
    private val valueHistory = mutableListOf<ValueWithMark>()                    // all value changes

    private var currentStartMark: TimeSource.Monotonic.ValueTimeMark? = null    // for persistedFor
    var lastTrueMark: TimeSource.Monotonic.ValueTimeMark? = null        // for timeSinceLastTrue
    var stableSince: TimeSource.Monotonic.ValueTimeMark                 // for wasStableFor
    private var lastValue: Boolean                                              // current value

    private var maxRequiredDuration: Duration = Duration.ZERO

    init {
        val now = clock.markNow()
        val initial = flow.value

        lastValue = initial
        stableSince = now
        valueHistory.add(ValueWithMark(initial, now))

        if (initial) {
            currentStartMark = now
            lastTrueMark = now
            timestamps.add(now)
        }

        RunixRuntimeScope.scope.launch {
            flow.collect { value ->
                logger.debug("newValue=$value, lastValue=$lastValue")
                val now = clock.markNow()

                if (value != lastValue) {
                    stableSince = now
                    valueHistory.add(ValueWithMark(value, now))
                }

                lastValue = value

                if (value) {
                    currentStartMark = now
                    lastTrueMark = now
                    timestamps.add(now)
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
        if (!lastValue) return ConditionEval.False

        val persisted = currentStartMark?.elapsedNow() ?: return ConditionEval.False
        logger.debug("persisted=$persisted")
        return if (persisted >= duration) {
            ConditionEval.True
        } else {
            ConditionEval.Delayed(clock.markNow().plus(duration - persisted))
        }
    }

    fun evaluateStability(duration: Duration): ConditionEval {
        pruneHistory(duration)

        val elapsed = stableSince.elapsedNow()
        return if (elapsed >= duration) {
            ConditionEval.True
        } else {
            ConditionEval.Delayed(stableSince.plus(duration - elapsed))
        }
    }

    fun countInWindow(window: Duration): Int {
        timestamps.removeIf { it.elapsedNow() > window }
        return timestamps.count()
    }

    fun timeSinceLastTrue(): Duration {
        return when {
            lastTrueMark != null -> lastTrueMark!!.elapsedNow()
            lastValue == true -> Duration.ZERO
            else -> Duration.INFINITE
        }
    }

    fun timeSinceChange(): Duration {
        return valueHistory.lastOrNull()?.timestamp?.elapsedNow() ?: Duration.INFINITE
    }
}