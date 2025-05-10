package runix.temporal.time

import java.time.Instant
import kotlin.time.ComparableTimeMark

// Convert a TimeMark to an Instant
fun ComparableTimeMark.toInstant(): Instant {
    val now = Instant.now()
    val remainingNanos = (this - Time.markNow()).inWholeNanoseconds
    return now.plusNanos(remainingNanos)
}
