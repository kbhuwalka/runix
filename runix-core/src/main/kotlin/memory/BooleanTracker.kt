package runix.core.logging.memory

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class BooleanTracker(flow: Flow<Boolean>) {
    private val timestamps = mutableListOf<TimeMark>()
    private var currentStartMark: TimeMark? = null
    private var lastTrueMark: TimeMark? = null
    private val clock = TimeSource.Monotonic

    init {
        CoroutineScope(Dispatchers.Default).launch {
            flow.collect { value ->
                if (value) {
                    if (currentStartMark == null) {
                        currentStartMark = clock.markNow()
                    }
                    lastTrueMark = clock.markNow()
                    timestamps.add(clock.markNow())
                } else {
                    currentStartMark = null
                }
            }
        }
    }

    fun getPersistedDuration(): Duration {
        return currentStartMark?.elapsedNow() ?: Duration.ZERO
    }

    fun countInWindow(window: Duration): Int {
        val now = clock.markNow()
        timestamps.removeIf { it.elapsedNow() > window }
        return timestamps.count()
    }

    fun timeSinceLastTrue(): Duration {
        return lastTrueMark?.elapsedNow() ?: Duration.INFINITE
    }
}