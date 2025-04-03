package runix.monitor

import kotlinx.coroutines.flow.Flow
import runix.core.logging.memory.BooleanTracker
import runix.core.logging.memory.NumericTracker
import java.util.concurrent.ConcurrentHashMap

object TemporalEngine {
    private val boolTrackers = ConcurrentHashMap<String, BooleanTracker>()
    private val numericTrackers = ConcurrentHashMap<String, NumericTracker>()

    fun trackBoolean(flow: Flow<Boolean>): BooleanTracker {
        val key = flow.toString()
        return boolTrackers.computeIfAbsent(key) {
            BooleanTracker(flow)
        }
    }

    fun trackNumeric(flow: Flow<Double>): NumericTracker {
        val key = flow.toString()
        return numericTrackers.computeIfAbsent(key) {
            NumericTracker(flow)
        }
    }
}