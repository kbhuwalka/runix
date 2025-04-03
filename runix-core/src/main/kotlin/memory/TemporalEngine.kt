package runix.monitor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import runix.core.logging.memory.BooleanTracker
import runix.core.logging.memory.NumericTracker
import java.util.concurrent.ConcurrentHashMap

object TemporalEngine {
    private val boolTrackers = ConcurrentHashMap<String, BooleanTracker>()
    private val numericTrackers = ConcurrentHashMap<String, NumericTracker>()

    // Preferred: named boolean flow
    fun trackBoolean(flow: Flow<Boolean>, key: String): BooleanTracker {
        return boolTrackers.computeIfAbsent(key) {
            val hotFlow = if (flow is StateFlow) flow else flow.ensureHot(key)
            BooleanTracker(hotFlow)
        }
    }

    // Fallback: warn about non-named flow
    fun trackNumeric(flow: Flow<Double>, key: String): NumericTracker {
        return numericTrackers.computeIfAbsent(key) {
            NumericTracker(flow)
        }
    }
}

private val sharedScope = CoroutineScope(Dispatchers.Default)

fun Flow<Boolean>.ensureHot(name: String): StateFlow<Boolean> {
    return this.stateIn(sharedScope, SharingStarted.Eagerly, false)
}