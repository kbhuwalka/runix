package runix.temporal

import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap

object TemporalEngine {
    private val boolTrackers = ConcurrentHashMap<String, BooleanTracker>()
    private val numericTrackers = ConcurrentHashMap<String, NumericTracker>()

    // Preferred: named boolean flow
    fun trackBoolean(flow: StateFlow<Boolean>, key: String): BooleanTracker {
        return boolTrackers.computeIfAbsent(key) {
            BooleanTracker(flow)
        }
    }

    // Fallback: warn about non-named flow
    fun trackNumeric(flow: StateFlow<Double>, key: String): NumericTracker {
        return numericTrackers.computeIfAbsent(key) {
            NumericTracker(flow)
        }
    }
}
