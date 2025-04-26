package runix.temporal

import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

data class CompiledMonitor(
    val compiledCondition: TemporalExpression,
    val flowRegistrations: List<FlowRegistration>
) {
    fun startTracking(onSignalUpdate: () -> Unit) {
        for (registration in flowRegistrations) {
            TemporalEngine.trackBoolean(
                registration.flow,
                registration.key,
                registration.requiredRetention,
                onSignalUpdate
            )
        }
    }

    fun stopTracking() {
        flowRegistrations.forEach { TemporalEngine.untrackBoolean(it.key) }
    }
}

data class FlowRegistration(
    val flow: StateFlow<Boolean>,
    val key: String,
    val requiredRetention: Duration
)