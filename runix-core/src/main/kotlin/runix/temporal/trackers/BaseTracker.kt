package runix.temporal.trackers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import runix.runtime.internal.RuntimeScope
import runix.temporal.time.Time
import kotlin.time.Duration

/**
 * Base tracker for temporal event series.
 *
 * @param T The type of values being tracked
 * @param flow Source of values to track
 * @param retention How long to retain historical values
 * @param scope Coroutine scope for collecting values
 * @param onUpdate Callback for value updates
 *
 * @throws IllegalStateException if the flow is already closed
 * @throws IllegalArgumentException if retention is negative
 *
 * Example:
 * ```
 * val tracker = NumericTracker(temperatureFlow, 1.hours)
 * tracker.evaluateTrend { current, previous -> current > previous }
 * ```
 */
internal abstract class BaseTracker<T>(
    flow: StateFlow<T>,
    retention: Duration
) {

    abstract fun registerWith(key: String)

    /** Time-stamped history buffer, seeded with the current value */
    protected val history: ValueHistory<T> =
        ValueHistory(
            initial = ValueWithMark(flow.value, Time.markNow()),
            retention = retention
        )

    private val job by lazy {
        RuntimeScope.scope.launch {
            try {
                flow.collect { value ->
                    val mark = Time.markNow()
                    history.append(value, mark)
                }
            } catch (e: Throwable) {
                // If the flow itself throws, rethrow or optionally trace/log here.
                throw e
            }
        }
    }

    /**
     * Stops tracking this flow - cancels the collector and no further events will be recorded
     */
    internal open fun stop() {
        job?.cancel()
    }

    /**
     * Permanently drops all recorded events.
     * After calling this, `hasFluctuated()`, etc. will operate only on new data.
     */
    open fun clearHistory() {
        history.clear()
    }
}
