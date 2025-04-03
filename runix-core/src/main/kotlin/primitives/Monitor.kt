package runix.primitives

import kotlinx.coroutines.flow.StateFlow
import runix.core.logging.primitives.MonitorContext
import runix.core.logging.primitives.RunixExecutionContext
import runix.memory.ConditionEval
import runix.memory.TemporalExpression
import kotlin.time.Duration

abstract class Monitor protected constructor(
    open val name: String,
    open val dependsOn: List<StateFlow<*>>,
    open val condition: () -> ConditionEval,
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

        when (result) {
            is ConditionEval.True -> {
                val throttle = throttleInterval
                if (throttle != null && !MonitorThrottleRegistry.shouldEmit(name, throttle)) {
                    context.logSkipped(name, "Throttled due to $throttle")
                    return
                }
                onTriggered(context)
            }

            is ConditionEval.False -> {
                onSkipped(context)
            }

            is ConditionEval.Delayed -> {
                // No-op: scheduler will handle rescheduling
                onSkipped(context)
            }
        }
    }
}