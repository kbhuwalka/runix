package runix.temporal

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import runix.internal.RuntimeScope
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * Tracks boolean signal transitions over time with temporal retention.
 * Maintains a history of value changes and provides temporal condition evaluations.
 */
internal class BooleanTracker(
    flow: StateFlow<Boolean>,
    private val retentionDuration: Duration,
    private val onUpdate: () -> Unit
) {
    private val clock = TimeSource.Monotonic

    /**
     * Represents a value change event with its timestamp.
     * Consecutive duplicates are not recorded.
     */
    internal data class ValueWithMark(val value: Boolean, val timestamp: TimeSource.Monotonic.ValueTimeMark)

    /**
     * Encapsulates the history of value transitions with append and pruning logic.
     */
    internal class ValueHistory(private val retention: Duration) {
        private val events = ArrayDeque<ValueWithMark>()
        val size: Int
            get() = events.size

        /**
         * Append a new value event if it differs from the last recorded value.
         */
        fun append(value: Boolean, timestamp: TimeSource.Monotonic.ValueTimeMark) {
            if (retention == Duration.ZERO) {
                events.clear()
                events.add(ValueWithMark(value, timestamp))
                return
            }

            if (events.isEmpty() || events.last().value != value) {
                events.addLast(ValueWithMark(value, timestamp))
                prune(timestamp)
            }
        }

        /**
         * Prune events to retain only those overlapping with the retention window.
         * Open-ended last period is always preserved.
         */
        fun prune(now: TimeSource.Monotonic.ValueTimeMark) {
            if (retention == Duration.ZERO) return

            val windowStart = now - retention
            while (events.size > 1) {
                val first = events.first()
                val second = events.elementAt(1)
                if (first.timestamp >= windowStart || second.timestamp >= windowStart) {
                    break
                }
                events.removeFirst()
            }
        }

        fun last(): ValueWithMark {
            return events.lastOrNull() ?: error("ValueHistory unexpectedly empty: invariant violated")
        }

        /**
         * Provides a snapshot of the current events.
         */
        fun entries(): Iterable<ValueWithMark> = events
    }

    internal val valueHistory = ValueHistory(retentionDuration)
    private val job: Job

    init {
        val now = clock.markNow()
        valueHistory.append(flow.value, now)

        job = RuntimeScope.scope.launch {
            flow.collect { value ->
                val now = clock.markNow()
                valueHistory.append(value, now)
                onUpdate()
            }
        }
    }

    fun dispose() {
        job.cancel()
    }

    fun isTrue(): ConditionEval =
        if (valueHistory.last().value) ConditionEval.True else ConditionEval.False

    fun hasPersistedFor(duration: Duration): ConditionEval {
        val now = clock.markNow()
        val lastEvent = valueHistory.last()

        if (!lastEvent.value) return ConditionEval.False

        val elapsed = now - lastEvent.timestamp
        return if (elapsed >= duration) {
            ConditionEval.True
        } else {
            ConditionEval.Delayed(lastEvent.timestamp.plus(duration - elapsed))
        }
    }

    fun hasBeenStableFor(duration: Duration): ConditionEval {
        val stableSince = valueHistory.last().timestamp
        val elapsed = stableSince.elapsedNow()
        return if (elapsed >= duration) {
            ConditionEval.True
        } else {
            ConditionEval.Delayed(stableSince.plus(duration - elapsed))
        }
    }

    fun hasPersistedForAtLeast(target: Duration): ConditionEval {
        val now = clock.markNow()
        val windowStart = now - retentionDuration

        valueHistory.prune(now) // Always prune first!
        val history = valueHistory.entries()

        var totalTrueDuration = Duration.ZERO
        var currentTrueStart: TimeSource.Monotonic.ValueTimeMark? = null

        for (entry in history) {
            if (entry.value) {
                if (currentTrueStart == null) {
                    // Open true period — snap start to windowStart if necessary
                    currentTrueStart = maxOf(entry.timestamp, windowStart)
                }
            } else {
                if (currentTrueStart != null) {
                    totalTrueDuration += entry.timestamp - currentTrueStart
                    currentTrueStart = null
                }
            }
        }

        // If still in an open true period, count up to now
        if (currentTrueStart != null) {
            totalTrueDuration += now - currentTrueStart
        }

        return if (totalTrueDuration >= target) {
            ConditionEval.True
        } else {
            ConditionEval.Delayed(windowStart.plus(target - totalTrueDuration))
        }
    }

    fun wasEverTrue(): ConditionEval {
        val now = clock.markNow()

        valueHistory.prune(now)
        val history = valueHistory.entries()

        val hasTrue = history.any { it.value }
        return if (hasTrue) {
            ConditionEval.True
        } else {
            ConditionEval.Delayed(now.plus(retentionDuration))
        }
    }

    /**
     * Checks if a fluctuation (true ➔ false or false ➔ true) has occurred within the retention window.
     *
     * Due to cognitive model invariants:
     * - ValueHistory only records transitions (no consecutive duplicates).
     * - Thus, if size >= 2, a fluctuation must have occurred.
     * - If size < 2, no fluctuation is possible yet.
     *
     * This allows an O(1) evaluation without scanning history.
     */
    fun hasFluctuated(): ConditionEval {
        val now = clock.markNow()

        valueHistory.prune(now)

        return if (valueHistory.size < 2) {
            ConditionEval.Delayed(now.plus(retentionDuration))
        } else {
            ConditionEval.True
        }
    }
}