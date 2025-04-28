package runix.temporal.time

import java.time.Instant
import kotlin.time.ComparableTimeMark

/**
 * Provides monotonic and wall-clock time for Runix.
 * All internal temporal logic should call Time.markNow() or Time.nowWallClock().
 */
internal interface TimeProvider {
    /** Monotonic “now,” suitable for all delay/expiry logic. */
    fun markNow(): ComparableTimeMark

    /** Wall-clock timestamp for logging and display. */
    fun nowWallClock(): Instant
}