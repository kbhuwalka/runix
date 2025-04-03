import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import runix.dsl.monitor
import runix.memory.persistedFor
import kotlin.time.Duration.Companion.seconds

object Monitors {
    fun LowSpeedDuringCalibration() = monitor("LowSpeedDuringCalibration") {
        dependsOn(BotState.isCalibrating, BotState.currentSpeed)

        condition {
            BotState.isLowSpeedDuringCalibration
                .persistedFor(2.seconds, key = "low_speed_calibration")
                .invoke()
        }

        throttle(20.seconds)
        trigger(RobotSignal.CalibrationWarning)
    }

    fun OverheatMonitor() = monitor("OverheatMonitor") {
        dependsOn(BotState.temperature)

        condition {
            BotState.temperature
                .map { it > 85 }
                .persistedFor(3.seconds, key = "temperature")
                .invoke()
        }

        throttle(1.seconds)
        trigger(RobotSignal.OverheatWarning)
    }

    fun BatteryMonitor() = monitor("BatteryLow") {
        dependsOn(BotState.batteryLevel)

        condition {
            BotState.batteryLevel
                .map { it < 25 }
                .persistedFor(1.seconds, key = "low_battery")
                .invoke()
        }

        throttle(10.seconds)
        trigger(RobotSignal.BatteryLow)
    }
}