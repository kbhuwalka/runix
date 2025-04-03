import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import runix.memory.persistsFor
import runix.primitives.Monitor
import kotlin.time.Duration.Companion.seconds

object Monitors {
    fun LowSpeedDuringCalibration() = object : Monitor(
        name = "LowSpeedDuringCalibration",
        condition = {
            BotState.isLowSpeedDuringCalibration.persistsFor(2.seconds, "low_speed_calibration")()
        },
        trigger = RobotSignal.CalibrationWarning,
        throttleInterval = 20.seconds
    ) {}

    fun OverheatMonitor() = object : Monitor(
        name = "OverheatMonitor",
        condition =  BotState.temperature
                        .map { it > 85 }
                        .persistsFor(3.seconds, "temperature"),
        trigger = RobotSignal.OverheatWarning,
        throttleInterval = 20.seconds
    ) {}

    fun BatteryMonitor() = object : Monitor(
        name = "BatteryLow",
        condition = {
            BotState.batteryLevel
                .map { it < 25 }
                .persistsFor(1.seconds, "low_battery")()
        },
        trigger = RobotSignal.BatteryLow,
        throttleInterval = 20.seconds
    ) {}
}