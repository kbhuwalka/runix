package runix.temporal.time

import java.time.Instant
import kotlin.time.ComparableTimeMark

/**
 * Global facade for time. By default, uses RealTimeProvider,
 * but can be swapped for a custom one (e.g. for tests or simulation).
 */
internal object Time {
    private var provider: TimeProvider = RealTimeProvider()

    /** Monotonic “now”—marks are ComparableTimeMark, so you can use `<`, `>`, `-`, and `+`. */
    fun markNow(): ComparableTimeMark =
        provider.markNow()

    /** Wall-clock now for human output. */
    fun nowWallClock(): Instant =
        provider.nowWallClock()

    /** Swap in a different provider (e.g. a virtual clock). */
    fun setProvider(newProvider: TimeProvider) {
        provider = newProvider
    }

    /** Restore the default real/time behavior. */
    fun resetToRealTime() {
        provider = RealTimeProvider()
    }
}
