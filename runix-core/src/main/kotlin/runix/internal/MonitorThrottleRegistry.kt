package runix.internal

import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration

sealed class ThrottleResult {
    object Allow : ThrottleResult()
    data class Throttled(val timeRemainingMs: Long) : ThrottleResult()
}

internal object MonitorThrottleRegistry {
    private val lastTriggerTimes = ConcurrentHashMap<String, Long>()

    fun check(name: String, interval: Duration): ThrottleResult {
        val now = System.currentTimeMillis()
        val last = lastTriggerTimes[name]
        return if (last == null || now - last >= interval.inWholeMilliseconds) {
            lastTriggerTimes[name] = now
            ThrottleResult.Allow
        } else {
            val remaining = interval.inWholeMilliseconds - (now - last)
            ThrottleResult.Throttled(remaining)
        }
    }
}
