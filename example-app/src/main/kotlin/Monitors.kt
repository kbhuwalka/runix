import runix.primitives.Monitor

object Monitors {
    fun LowSpeedDuringCalibration() = object : Monitor(
        name = "LowSpeedDuringCalibration",
        condition = {
            BotState.isCalibrating.value &&
                    BotState.currentSpeed.value < 5
        },
        trigger = RobotSignal.CalibrationWarning
    ) {}

    fun OverheatMonitor() = object : Monitor(
        name = "OverheatMonitor",
        condition = {
            BotState.temperature.value > 85
        },
        trigger = RobotSignal.OverheatWarning
    ) {}

    fun BatteryMonitor() = object : Monitor(
        name = "BatteryLow",
        condition = {
            BotState.batteryLevel.value < 25
        },
        trigger = RobotSignal.BatteryLow
    ) {}
}