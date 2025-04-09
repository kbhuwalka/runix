package robot_cleaner.monitors

import robot_cleaner.signals.Signals
import robot_cleaner.state.PowerState
import runix.dsl.monitor
import runix.primitives.Monitor
import runix.temporal.asCondition
import runix.temporal.persistedFor
import kotlin.time.Duration.Companion.seconds

object BatteryMonitors {

    val lowBatteryMonitor: Monitor = monitor("LowBatteryMonitor") {
        dependsOn(PowerState.isBatteryLow)
        fireIf(PowerState.isBatteryLow.persistedFor(5.seconds, "battery-low"))
        emits(Signals.returnToBase)
        throttle(30.seconds)
    }

    val batteryFullMonitor: Monitor = monitor("BatteryFullMonitor") {
        dependsOn(PowerState.isBatteryFull)
        fireIf(PowerState.isBatteryFull.asCondition("battery-full"))
        emits(Signals.startCleaning)
    }

    fun all(): List<Monitor> = listOf(
        lowBatteryMonitor,
        batteryFullMonitor
    )
}