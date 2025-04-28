package runix.temporal.trackers

import kotlin.time.ComparableTimeMark
import kotlin.time.Duration

/**
 * A single value with its monotonic timestamp.
 */
internal data class ValueWithMark<T>(
    val value: T,
    val timestamp: ComparableTimeMark
)

/**
 * Maintains a time-stamped history of values of type [T], pruned by a retention window.
 *
 * Invariant: history is never empty—constructed with an initial event.
 *
 * @param initial   the first value event to seed history.
 * @param retention time window to retain history; zero means “always keep only most recent value.”
 */
internal class ValueHistory<T>(
    initial: ValueWithMark<T>,
    private val retention: Duration
) {
    private val events = ArrayDeque<ValueWithMark<T>>()

    init {
        require(!retention.isNegative()) { "retention must be ≥ 0, was $retention" }
        // seed your history:
        events.add(initial)
    }

    /** Number of retained events (always ≥ 1). */
    val size: Int
        get() = events.size

    /**
     * Append a new value event if it differs from the last recorded value.
     * If [retention] is zero, keep only the most recent event.
     *
     * @param value     the new signal value
     * @param timestamp monotonic timestamp of the event
     */
    fun append(value: T, timestamp: ComparableTimeMark) {
        val last = events.last()
        if (last.value == value) return

        if (retention == Duration.ZERO) {
            events.clear()
        }

        events.addLast(ValueWithMark(value, timestamp))
        prune(timestamp)
    }

    /**
     * Prune events older than the retention window, but never remove the very last event.
     *
     * @param now reference timestamp for the retention cutoff
     */
    fun prune(now: ComparableTimeMark) {
        if (retention == Duration.ZERO) return

        val windowStart = now - retention
        while (events.size > 1 && events[1].timestamp <= windowStart) {
            events.removeFirst()
        }
    }

    /**
     * All retained events in chronological order.
     * Always non-empty due to constructor seeding.
     */
    fun entries(): Iterable<ValueWithMark<T>> = events

    /**
     * Chronologically first event.
     */
    fun first(): ValueWithMark<T> = events.first()

    /**
     * Chronologically last (most recent) event.
     */
    fun last(): ValueWithMark<T> = events.last()
}