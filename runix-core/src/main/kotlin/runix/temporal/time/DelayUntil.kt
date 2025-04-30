package runix.temporal.time

import kotlinx.coroutines.delay
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration.Companion.ZERO

/**
 * Suspend until the given [mark] is reached.
 * In tests (with a TestCoroutineScheduler) or in real time,
 * this hooks into the same clock as Time.markNow().
 */
suspend fun delayUntil(mark: ComparableTimeMark) {
    val wait = mark.durationSince(Time.markNow())
    if (wait > ZERO) {
        delay(wait)
    }
}