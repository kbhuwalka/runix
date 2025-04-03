package runix.core.logging.memory

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

class FlowTracker(private val source: Flow<Boolean>) {
    private val timestamps = mutableListOf<Long>()
    private var currentStartTime: Long? = null
    private val clock = TimeSource.Monotonic
    private val mutex = Mutex()

    init {
        CoroutineScope(Dispatchers.Default).launch {
            source.collect { value ->
                val now = clock.markNow().elapsedNow().inWholeMilliseconds
                mutex.withLock {
                    if (value) {
                        if (currentStartTime == null) currentStartTime = now
                        timestamps.add(now)
                    } else {
                        currentStartTime = null
                    }
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
        return timestamps.count { it >= cutoff }
    }
}