package runix.monitor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import runix.core.logging.memory.BooleanTracker
import runix.core.logging.memory.NumericTracker
import java.util.concurrent.ConcurrentHashMap

object TemporalEngine {
    private val boolTrackers = ConcurrentHashMap<String, BooleanTracker>()
    private val numericTrackers = ConcurrentHashMap<String, NumericTracker>()

    // Preferred: named boolean flow
    fun trackBoolean(flow: Flow<Boolean>, key: String): BooleanTracker {
        return boolTrackers.computeIfAbsent(key) {
            BooleanTracker(flow)
        }
    }

    // Fallback: warn about non-named flow
    fun trackNumeric(flow: Flow<Double>, key: String): NumericTracker {
        return numericTrackers.computeIfAbsent(key) {
            NumericTracker(flow)
        }
    }
}