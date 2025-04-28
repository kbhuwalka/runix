package runix.temporal

import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

/**
 * The result of compiling a [MonitoredCondition]:
 *  - [name]: the name of the compiled monitor.
 *  - [compiledCondition]: the runtime evaluator (TemporalExpression).
 *  - [flowRegistrations]: the list of flows + keys + retention windows to register.
 *
 */
internal data class CompiledMonitor(
    val name: String,
    val compiledCondition: TemporalExpression,
    val flowRegistrations: List<FlowRegistration>
) {

    /**
     * Starts tracking all registered boolean flows in the engine.
     *
     * @param onSignalUpdate invoked whenever any tracked signal updates
     */
    internal fun startTracking(onSignalUpdate: () -> Unit) {
        for (registration in flowRegistrations) {
            TemporalEngine.trackBoolean(
                registration.flow,
                registration.key,
                registration.requiredRetention,
                onSignalUpdate
            )
        }
    }

    /**
     * Stops tracking all registered flows.
     */
    internal fun stopTracking() {
        flowRegistrations.forEach {
            TemporalEngine.untrackBoolean(it.key)
        }
    }
}

/**
 * Associates a boolean [flow] with:
 *  - a unique tracker [key], and
 *  - the [requiredRetention] window needed to evaluate its condition.
 *
 * Internal to the Runix framework.
 */
internal data class FlowRegistration(
    val flow: StateFlow<Boolean>,
    val key: String,
    val requiredRetention: Duration
)