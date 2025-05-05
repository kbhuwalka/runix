package runix.primitives.monitor

import kotlinx.coroutines.flow.MutableStateFlow
import runix.primitives.signal.SignalHandle
import runix.primitives.signal.signal
import runix.temporal.MonitoredCondition
import runix.temporal.dsl.hasBeenTrueFor
import kotlin.time.Duration.Companion.seconds

/**
 * Creates a declarative monitor that emits an optional signal
 * when its condition evaluates to true.
 */
fun monitor(name: String, condition: () -> MonitoredCondition): MonitorHandleBuilder {
    return MonitorHandleBuilder(name, condition())
}

/**
 * Builder pattern to allow `.emit(...)` chaining.
 */
class MonitorHandleBuilder internal constructor(
    private val name: String,
    private val condition: MonitoredCondition
) {
    infix fun emits(signal: SignalHandle<Unit>): MonitorHandle {
        return MonitorHandle(name, condition, signal)
    }
}