package runix.temporal.trackers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import runix.internal.RuntimeScope
import runix.temporal.time.Time
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration

/**
 * Core tracker functionality for any StateFlow<T> signal:
 * - Seeds with an initial value event
 * - Collects subsequent signal values over time
 * - Maintains a deque of timestamped events with a fixed retention window
 * - Provides access to the retained history for temporal queries
 *
 * @param T type of the signal values
 * @param flow the StateFlow<T> to track
 * @param retention time window for pruning history; zero means “always keep only latest change”
 */
internal abstract class BaseTracker<T>(
    flow: StateFlow<T>,
    retention: Duration,
    /**
    * Collector scope—defaults to the global RuntimeScope but can be overridden
    * in tests or alternate contexts.
    */
    private val scope: CoroutineScope = RuntimeScope.scope
) {

    /** Time-stamped history buffer, seeded with the current value */
    protected val history: ValueHistory<T> =
        ValueHistory(
            initial   = ValueWithMark(flow.value, Time.markNow()),
            retention = retention
        )

    private val job: Job = scope.launch {
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

    /**
     * Stops tracking this flow - cancels the collector and no further events will be recorded
     */
    internal fun stop() {
        job.cancel()
    }
}