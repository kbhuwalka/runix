package runix.internal

import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration

internal object MonitorThrottleRegistry {
    private val lastTriggerTimes = ConcurrentHashMap<String, Long>()

    fun shouldEmit(name: String, interval: Duration): Boolean {
        val now = System.currentTimeMillis()
        val last = lastTriggerTimes[name]
        return if (last == null || now - last >= interval.inWholeMilliseconds) {
            lastTriggerTimes[name] = now
            true
        } else {
            false
        }
    }
}
