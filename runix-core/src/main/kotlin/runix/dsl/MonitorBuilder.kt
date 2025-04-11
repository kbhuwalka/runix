package runix.dsl

import runix.primitives.Monitor
import runix.primitives.Signal
import runix.temporal.MonitoredCondition
import kotlin.time.Duration

class MonitorBuilder internal constructor(private val name: String) {
    private var conditionTree: MonitoredCondition? = null
    private var trigger: Signal? = null
    private var throttle: Duration? = null

    fun fireIf(condition: MonitoredCondition) {
        this.conditionTree = condition
    }

    fun emits(signal: Signal) {
        this.trigger = signal
    }

    fun throttle(duration: Duration) {
        this.throttle = duration
    }

    fun build(): Monitor {
        require(conditionTree != null) { "Monitor '$name' must define a condition." }
        require(trigger != null) { "Monitor '$name' must define a trigger signal." }

        return object : Monitor(
            name = name,
            conditionTree = conditionTree!!,
            trigger = trigger!!,
            throttleInterval = throttle
        ) {}
    }
}

fun monitor(name: String, block: MonitorBuilder.() -> Unit): Monitor {
    return MonitorBuilder(name).apply(block).build()
}
