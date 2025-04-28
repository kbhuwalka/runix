package runix.internal

import runix.temporal.time.Time
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.TimeMark

sealed class ThrottleResult {
    object Allow : ThrottleResult()
    data class Throttled(val timeRemainingMs: Long) : ThrottleResult()
}

internal object MonitorThrottleRegistry {
    private val lastTriggerMarks = ConcurrentHashMap<String, TimeMark>()

    fun peek(key: String, interval: Duration): ThrottleResult {
        val last = lastTriggerMarks[key]
        return if (last == null || last.elapsedNow() >= interval) {
            ThrottleResult.Allow
        } else {
            val remaining = interval - last.elapsedNow()
            ThrottleResult.Throttled(remaining.inWholeMilliseconds)
        }
    }

    fun recordTrigger(key: String) {
        lastTriggerMarks[key] = Time.markNow()
    }
}
