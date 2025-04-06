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

    open fun onTriggered(context: MonitorContext) {
        context.logTriggered(name, "Firing signal ${trigger.name}")
        context.emit(trigger)
    }

    open fun onSkipped(context: MonitorContext) {
        context.logSkipped(name, "Skipped")
    }

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
                if (throttle != null) {
                    when (val throttleResult = MonitorThrottleRegistry.check(name, throttle)) {
                        is ThrottleResult.Throttled -> {
                            context.logSkipped(name, "Throttled for ${throttleResult.timeRemainingMs}ms")
                            return
                        }
                        ThrottleResult.Allow -> {}
                    }
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
