package runix.temporal.trackers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import runix.runtime.internal.RuntimeScope
import runix.temporal.condition.ConditionEval
import runix.temporal.time.Time
import runix.temporal.time.durationSince
import kotlin.time.Duration

/**
 * Tracker for categorical signals (e.g., Enum states, String modes).
 *
 * Supports:
 * - Instantaneous transition detection (entered or exited a state)
 * - Dwell-time persistence validation
 * - Ordered sequence transition validation
 *
 * All evaluation is bounded by the configured retention window.
 */
internal open class CategoricalTracker<T>(
    flow: StateFlow<T>,
    retention: Duration,
    scope: CoroutineScope = RuntimeScope.scope,
    onUpdate: () -> Unit
) : BaseTracker<T>(flow, retention, scope, onUpdate) {

    override fun registerWith(key: String) {
        TrackerRegistry.register(key, this)
    }

    /**
     * Evaluates if the latest value matches [match] predicate,
     * and has been held continuously for at least [forTime].
     *
     * - Returns [ConditionEval.True] if satisfied.
     * - Returns [ConditionEval.False] if value doesn't match.
     * - Returns [ConditionEval.Delayed] if dwell-time not yet satisfied.
     */
    internal fun evaluateLatestPersisted(
        match: (value: T) -> Boolean,
        forTime: Duration
    ): ConditionEval {
        val now = Time.markNow()
        history.prune(now)

        val last = history.last()
        if (!match(last.value)) {
            return ConditionEval.False
        }

        val elapsed = now - last.timestamp
        return if (elapsed >= forTime) {
            ConditionEval.True
        } else {
            ConditionEval.Delayed(last.timestamp.plus(forTime))
        }
    }

    /**
     * Evaluates if a transition matching [match] predicate occurred
     * between adjacent events, and the resulting state was held for at least [forTime].
     *
     * - Returns [ConditionEval.True] if dwell-time satisfied.
     * - Returns [ConditionEval.Delayed] if still dwelling but not satisfied.
     * - Returns [ConditionEval.False] otherwise.
     */
    internal fun evaluatePastTransition(
        match: (prev: T, curr: T) -> Boolean,
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
            if (match(prev.value, curr.value)) {
                if (dwellDuration >= forTime) {
                    return ConditionEval.True
                } else if (!iterator.hasNext()) {
                    return ConditionEval.Delayed(curr.timestamp.plus(forTime))
                }
                // else: continue scanning, maybe find a better match
            }

            prev = curr
        }

        val elapsed = now.durationSince(prev.timestamp)
        if (match(prev.value, prev.value)) {
            return if (elapsed >= forTime) {
                ConditionEval.True
            } else {
                ConditionEval.Delayed(prev.timestamp.plus(forTime))
            }
        }

        // No matches found, need to wait for new events
        return ConditionEval.False
    }

    /**
     * Evaluates if the signal transitioned through the ordered [states] sequence
     * without interruptions.
     *
     * - Returns [ConditionEval.True] if the full sequence matched.
     * - Returns [ConditionEval.False] otherwise.
     */
    internal fun evaluateTransitionSequence(
        states: List<T>
    ): ConditionEval {
        val now = Time.markNow()
        history.prune(now, minSize = states.size)

        val entries = history.entries()
        if (entries.count() < states.size) return ConditionEval.False

        var index = 0
        for (entry in entries) {
            if (index >= states.size) {
                return ConditionEval.True
            }

            if (entry.value == states[index]) {
                index++
            } else {
                // Reset if sequence breaks
                index = if (entry.value == states[0]) 1 else 0
            }
        }

        if (index >= states.size) {
            return ConditionEval.True
        }

        return ConditionEval.False
    }
}
