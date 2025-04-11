package robot_cleaner.monitors

import robot_cleaner.signals.Signals
import robot_cleaner.state.PowerState
import runix.dsl.monitor
import runix.primitives.Monitor
import runix.temporal.persistedFor
import runix.temporal.whenTrue
import kotlin.time.Duration.Companion.seconds

object BatteryMonitors {

    val lowBatteryMonitor: Monitor = monitor("LowBatteryMonitor") {
        fireIf(PowerState.isBatteryLow.persistedFor(5.seconds))
        emits(Signals.returnToBase)
        throttle(30.seconds)
    }

    val batteryFullMonitor: Monitor = monitor("BatteryFullMonitor") {
        fireIf(PowerState.isBatteryFull.whenTrue())
        emits(Signals.startCleaning)
    }

    fun all(): List<Monitor> = listOf(
        lowBatteryMonitor,
        batteryFullMonitor
    )
}
