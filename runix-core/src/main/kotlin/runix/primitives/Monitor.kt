package runix.primitives

import kotlinx.coroutines.flow.StateFlow
import runix.temporal.MonitoredCondition
import runix.temporal.TemporalExpression
import kotlin.time.Duration

/**
 * Declarative monitor object that is compiled and registered by the scheduler.
 *
 * - `conditionTree` is provided at build time via DSL
 * - `condition` is compiled and assigned by the scheduler at registration time
 */
abstract class Monitor protected constructor(
    open val name: String,
    open val conditionTree: MonitoredCondition,
    open val trigger: Signal,
    open val throttleInterval: Duration? = null
) {
    /** Final condition expression assigned by scheduler. */
    internal lateinit var condition: TemporalExpression

    /** Dependencies inferred from conditionTree. Assigned at registration. */
    lateinit var dependencies: Set<StateFlow<*>>

    open fun isEnabled(): Boolean = true

    /** Called when the monitor fires. */
    open fun onTriggered(context: MonitorContext) {
        context.logTriggered(name, "Firing signal ${trigger.name}")
        context.emit(trigger)
    }

    /** Called when the monitor is evaluated but not triggered. */
    open fun onSkipped(context: MonitorContext) {
        context.logSkipped(name, "Skipped")
    }
}
