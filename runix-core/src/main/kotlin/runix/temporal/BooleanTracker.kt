package runix.temporal

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import runix.internal.RuntimeScope
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * Pure signal tracker that stores temporal history and updates
 * when the signal (StateFlow<Boolean>) changes.
 */
class BooleanTracker(
    flow: StateFlow<Boolean>,
    private val retentionDuration: Duration,
    private val onUpdate: () -> Unit
) {
    data class ValueWithMark(val value: Boolean, val timestamp: TimeSource.Monotonic.ValueTimeMark)

    private val clock = TimeSource.Monotonic

    val timestamps = mutableListOf<TimeSource.Monotonic.ValueTimeMark>() // all true values
    val valueHistory = mutableListOf<ValueWithMark>() // all value changes

    var currentStartMark: TimeSource.Monotonic.ValueTimeMark? = null // when last became true
    var lastTrueMark: TimeSource.Monotonic.ValueTimeMark? = null // last time it was true
    var stableSince: TimeSource.Monotonic.ValueTimeMark // when value last changed
    var lastValue: Boolean // current value

    private val job: Job

    init {
        val now = clock.markNow()
        stableSince = now
        lastValue = flow.value

        if (retentionDuration > Duration.ZERO) {
            valueHistory.add(ValueWithMark(lastValue, now))
        }

        if (lastValue) {
            lastTrueMark = now
            currentStartMark = now
            timestamps.add(now)
        }

        job = RuntimeScope.scope.launch {
            flow.collect { value ->
                val now = clock.markNow()

                if (value != lastValue) {
                    stableSince = now
                }

                if (value) {
                    lastTrueMark = now
                    timestamps.add(now)
                    if (!lastValue) {
                        currentStartMark = now
                    }
                } else {
                    currentStartMark = null
                }

                if (retentionDuration > Duration.ZERO) {
                    valueHistory.add(ValueWithMark(value, now))
                }

                lastValue = value
                pruneHistory(now)
                onUpdate()
            }
        }
    }

    private fun pruneHistory(now: TimeSource.Monotonic.ValueTimeMark) {
        valueHistory.removeIf { (now - it.timestamp) > retentionDuration }
        timestamps.removeIf { (now - it) > retentionDuration }
    }

    fun dispose() {
        job.cancel()
    }
}