package runix.dsl

import kotlinx.coroutines.flow.StateFlow
import runix.primitives.Monitor
import runix.primitives.Signal
import runix.temporal.ConditionEval
import runix.temporal.TemporalExpression
import kotlin.time.Duration

class MonitorBuilder internal constructor(private val name: String) {
    private var condition: TemporalExpression? = null
    private val dependencies = mutableListOf<StateFlow<*>>()
    private var trigger: Signal? = null
    private var throttle: Duration? = null

    fun dependsOn(vararg flows: StateFlow<*>) {
        dependencies += flows
    }

    fun fireIf(block: () -> Any) {
        this.condition = {
            when (val result = block()) {
                is Boolean -> if (result) ConditionEval.True else ConditionEval.False
                is ConditionEval -> result
                else -> error("Unsupported fireIf block return type: ${result.javaClass.name}. Use a Boolean or a TemporalExpression.")
            }
        }
    }

    fun throttle(duration: Duration) {
        this.throttle = duration
    }

    fun emits(signal: Signal) {
        this.trigger = signal
    }

    fun build(): Monitor {
        require(condition != null) { "Monitor '$name' must define a condition." }
        require(dependencies.isNotEmpty()) { "Monitor '$name' must define at least one dependency." }
        require(trigger != null) { "Monitor '$name' must define a trigger signal." }

        return object : Monitor(
            name = name,
            condition = condition!!,
            dependsOn = dependencies,
            trigger = trigger!!,
            throttleInterval = throttle
        ) {}
    }
}

fun monitor(name: String, block: MonitorBuilder.() -> Unit): Monitor {
    return MonitorBuilder(name).apply(block).build()
}
