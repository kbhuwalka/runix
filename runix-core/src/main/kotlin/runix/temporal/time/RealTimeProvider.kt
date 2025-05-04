package runix.temporal.time

import java.time.Instant
import kotlin.time.ComparableTimeMark
import kotlin.time.TimeSource

/** Production implementation, using the real monotonic clock and system wall-clock. */
internal class RealTimeProvider : TimeProvider {
    override fun markNow(): ComparableTimeMark =
        // TimeSource.Monotonic implements WithComparableMarks under the hood
        TimeSource.Monotonic.markNow()

    override fun nowWallClock(): Instant =
        Instant.now()
}
