package runix.core.logging.memory

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import runix.memory.ConditionEval
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.TimeSource

class BooleanTracker(flow: Flow<Boolean>) {
    private val timestamps = mutableListOf<TimeSource.Monotonic.ValueTimeMark>()
    private var currentStartMark: TimeSource.Monotonic.ValueTimeMark? = null
    private var lastTrueMark: TimeSource.Monotonic.ValueTimeMark? = null
    private val clock = TimeSource.Monotonic
    private val wallClock = java.time.Clock.systemUTC()

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

    fun evaluatePersistence(duration: Duration): ConditionEval {
        val persisted = currentStartMark?.elapsedNow() ?: return ConditionEval.False
        return if (persisted >= duration) {
            ConditionEval.True
        } else {
            val nextCheckAt = Instant.now(wallClock).plusMillis((duration - persisted).inWholeMilliseconds)
            ConditionEval.Delayed(nextCheckAt)
        }
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