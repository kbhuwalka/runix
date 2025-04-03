package runix.core.logging.memory

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

class BooleanTracker(flow: Flow<Boolean>) {
    private val timestamps = mutableListOf<Long>() // when true occurred
    private var currentStartTime: Long? = null
    private var lastTrueTime: Long? = null
    private val clock = TimeSource.Monotonic

    init {
        CoroutineScope(Dispatchers.Default).launch {
            flow.collect { value ->
                val now = clock.markNow().elapsedNow().inWholeMilliseconds
                if (value) {
                    if (currentStartTime == null) currentStartTime = now
                    lastTrueTime = now
                    timestamps.add(now)
                } else {
                    currentStartTime = null
                }
            }
        }
    }

    fun getPersistedDuration(): Duration {
        val now = clock.markNow().elapsedNow().inWholeMilliseconds
        val start = currentStartTime ?: return ZERO
        return (now - start).milliseconds
    }

    fun countInWindow(window: Duration): Int {
        val now = clock.markNow().elapsedNow().inWholeMilliseconds
        val cutoff = now - window.inWholeMilliseconds
        timestamps.removeIf { it < cutoff }
        return timestamps.count { it >= cutoff }
    }

    fun timeSinceLastTrue(): Duration {
        val now = clock.markNow().elapsedNow().inWholeMilliseconds
        val last = lastTrueTime ?: return INFINITE
        return (now - last).milliseconds
    }
}