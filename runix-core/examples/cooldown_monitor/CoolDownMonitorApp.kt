package examples.cooldown_monitor

import kotlinx.coroutines.flow.MutableStateFlow
import runix.core.RunixScheduler
import runix.dsl.monitor
import runix.primitives.Signal
import runix.temporal.asCondition
import kotlin.time.Duration.Companion.seconds

class CooldownMonitorApp {
    val binFull = MutableStateFlow(false)

    val signal = object : Signal("binFullDetected") {}

    val monitor = monitor("BinFullMonitor") {
        throttle(2.seconds)
        dependsOn(binFull)
        fireIf(binFull.asCondition("bin-full"))
        emits(signal)
    }

    fun registerWith(scheduler: RunixScheduler) {
        scheduler.register(monitor)
    }
}