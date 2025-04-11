package runix.temporal

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import runix.internal.RunixRuntimeScope
import kotlin.time.TimeSource

/**
 * Pure signal tracker that stores temporal history and updates
 * when the signal (StateFlow<Boolean>) changes.
 */
class BooleanTracker(
    flow: StateFlow<Boolean>,
    val onUpdate: () -> Unit
) {
    data class ValueWithMark(val value: Boolean, val timestamp: TimeSource.Monotonic.ValueTimeMark)

    private val clock = TimeSource.Monotonic

    val timestamps = mutableListOf<TimeSource.Monotonic.ValueTimeMark>() // all true values
    val valueHistory = mutableListOf<ValueWithMark>() // all value changes

    var currentStartMark: TimeSource.Monotonic.ValueTimeMark? = null // when last became true
    var lastTrueMark: TimeSource.Monotonic.ValueTimeMark? = null // last time it was true
    var stableSince: TimeSource.Monotonic.ValueTimeMark // when value last changed
    var lastValue: Boolean // current value

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

                onUpdate() // notify monitor re-eval
            }
        }
    }
}
