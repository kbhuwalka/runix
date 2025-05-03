package runix.primitives.monitor

import runix.primitives.signal.Signal
import runix.temporal.MonitoredCondition

/**
 * Creates a declarative monitor that emits an optional signal
 * when its condition evaluates to true.
 */
fun monitor(name: String, condition: MonitoredCondition): MonitorHandleBuilder {
    return MonitorHandleBuilder(name, condition)
}

/**
 * Builder pattern to allow `.emit(...)` chaining.
 */
class MonitorHandleBuilder internal constructor(
    private val name: String,
    private val condition: MonitoredCondition
) {
    infix fun emit(signal: Signal<Unit>): MonitorHandle {
        return MonitorHandle(name, condition, signal)
    }
}