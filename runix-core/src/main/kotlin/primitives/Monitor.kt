package runix.primitives

import runix.core.logging.primitives.MonitorContext
import runix.core.logging.primitives.RunixExecutionContext
import runix.memory.TemporalExpression
import kotlin.time.Duration

abstract class Monitor(
    open val name: String,
    open val condition: TemporalExpression,
    open val trigger: Signal,
    open val throttleInterval: Duration? = null
) {
    open fun isEnabled(): Boolean = true

    open fun onTriggered(context: MonitorContext) {
        context.logTriggered(name, "Firing signal ${trigger.name}")
        context.emit(trigger)
    }

    open fun onSkipped(context: MonitorContext) {}

    fun evaluateWithContext(baseCtx: RunixExecutionContext) {
        if (!isEnabled()) return

        val context = MonitorContext.from(baseCtx, name)

        val result = try {
            condition()
        } catch (e: Exception) {
            context.logSkipped(name, "Monitor [$name] threw error: ${e.message}")
            return
        }

        if (result) {
            val throttle = throttleInterval
            if (throttle != null && !MonitorThrottleRegistry.shouldEmit(name, throttle)) {
                return
            }
            onTriggered(context)
        } else {
            onSkipped(context)
        }
    }
}