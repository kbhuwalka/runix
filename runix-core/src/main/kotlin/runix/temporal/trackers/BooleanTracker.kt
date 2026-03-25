package runix.temporal.trackers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import runix.runtime.internal.RuntimeScope
import runix.temporal.condition.ConditionEval
import runix.temporal.time.Time
import runix.temporal.time.durationSince
import kotlin.time.Duration

/**
 * Internal tracker for boolean StateFlow signals.
 *
 * Collects timestamped changes, retains history within [retention],
 * and provides common temporal evaluations.
 *
 * History is pruned before each evaluation to bound memory usage.
 *
 * @param flow        the source boolean signal to track
 * @param retention   window of time to retain history events
 * @param scope       coroutine scope for collecting the flow
 * @param onUpdate    callback invoked on every recorded change
 */
internal open class BooleanTracker(
    flow: StateFlow<Boolean>,
    private val retention: Duration,
) : BaseTracker<Boolean>(flow, retention) {

    override fun registerWith(key: String) {
        TrackerRegistry.register(key, this)
    }

    /**
     * Evaluates whether the most recent value matches [match],
     * and has been held continuously for at least [forTime].
     *
     * - Returns [ConditionEval.True] if matched and dwell-time is satisfied.
     * - Returns [ConditionEval.False] if current value does not match.
     * - Returns [ConditionEval.Delayed] if match is still holding but dwell-time not yet satisfied.
     */
    internal fun evaluateLatestPersisted(
        match: (Boolean) -> Boolean,
        forTime: Duration
    ): ConditionEval {
        val now = Time.markNow()
        history.prune(now)

        val last = history.last()
        if (!match(last.value)) {
            return ConditionEval.False
        }

        val elapsed = now.durationSince(last.timestamp)
        return if (elapsed >= forTime) {
            ConditionEval.True
        } else {
            ConditionEval.Delayed(last.timestamp.plus(forTime))
        }
    }

    /**
     * Evaluates whether any historical value satisfies [match]
     * and remained true for at least [forTime].
     *
     * - Returns [ConditionEval.True] if dwell-time satisfied.
     * - Returns [ConditionEval.Delayed] if still holding but dwell not yet satisfied.
     * - Returns [ConditionEval.False] otherwise.
     */
    internal fun evaluatePastTransition(
        match: (Boolean) -> Boolean,
        forTime: Duration
    ): ConditionEval {
        val now = Time.markNow()
        history.prune(now)

        val iterator = history.entries().iterator()
        if (!iterator.hasNext()) return ConditionEval.False

        var prev = iterator.next()
        while (iterator.hasNext()) {
            val curr = iterator.next()

            val dwellDuration = curr.timestamp.durationSince(prev.timestamp)
            if (match(prev.value) && dwellDuration >= forTime) {
                return ConditionEval.True
            }

            prev = curr
        }

        if (!match(prev.value)) {
            return ConditionEval.False
        }

        val elapsed = now - prev.timestamp
        return if (elapsed >= forTime) {
            ConditionEval.True
        } else {
            ConditionEval.Delayed(prev.timestamp.plus(forTime))
        }
    }

    /**
     * Evaluates whether the boolean signal has ever changed
     * within the retention window.
     *
     * - Returns [ConditionEval.True] if a value transition was observed (prev ≠ curr).
     * - Returns [ConditionEval.False] otherwise.
     */
    fun evaluateFluctuated(): ConditionEval {
        val now = Time.markNow()
        history.prune(now)

        return if (history.size < 2) {
            ConditionEval.False
        } else {
            ConditionEval.True
        }
    }
}
