package runix.primitives

import kotlinx.coroutines.flow.StateFlow
import runix.internal.MonitorThrottleRegistry
import runix.internal.ThrottleResult
import runix.temporal.ConditionEval
import kotlin.time.Duration

abstract class Monitor protected constructor(
    open val name: String,
    open val dependsOn: List<StateFlow<*>>,
    open val condition: () -> ConditionEval,
    open val trigger: Signal,
    open val throttleInterval: Duration? = null
) {
    open fun isEnabled(): Boolean = true

    /**
     * Called when the monitor fires. Scheduler guarantees all throttling, evaluation, and delays are respected.
     */
    open fun onTriggered(context: MonitorContext) {
        context.logTriggered(name, "Firing signal ${trigger.name}")
        context.emit(trigger)
    }

    /**
     * Called when the monitor is evaluated but not triggered.
     */
    open fun onSkipped(context: MonitorContext) {
        context.logSkipped(name, "Skipped")
    }
}