package runix.temporal

import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration

internal object TemporalEngine {
    private val boolTrackers = ConcurrentHashMap<String, BooleanTracker>()
    private val numericTrackers = ConcurrentHashMap<String, NumericTracker>()

    fun trackBoolean(
        flow: StateFlow<Boolean>,
        key: String,
        retention: Duration,
        onUpdate: () -> Unit
    ): BooleanTracker {
        return boolTrackers.computeIfAbsent(key) {
            BooleanTracker(flow, retention, onUpdate)
        }
    }

    fun untrackBoolean(key: String) {
        boolTrackers.remove(key)?.dispose()
    }

    fun trackNumeric(flow: StateFlow<Double>, key: String): NumericTracker {
        return numericTrackers.computeIfAbsent(key) {
            NumericTracker(flow)
        }
    }

    fun getBooleanTracker(key: String): BooleanTracker {
        return boolTrackers[key]
            ?: error("BooleanTracker not registered for key: $key")
    }
}
