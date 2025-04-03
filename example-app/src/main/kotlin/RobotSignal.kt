import runix.primitives.Signal

sealed class RobotSignal(val id: String) : Signal(id) {
    object CalibrationStarted : RobotSignal("CalibrationStarted")
    object CalibrationWarning : RobotSignal("CalibrationWarning")
    object CalibrationFailed : RobotSignal("CalibrationFailed")
    object CalibrationSucceeded : RobotSignal("CalibrationSucceeded")
    object OverheatWarning : RobotSignal("OverheatWarning")
    object DeliverySuccess : RobotSignal("DeliverySuccess")
    object BatteryLow : RobotSignal("BatteryLow")
    object BatteryCharged : RobotSignal("BatteryCharged")
    object ReturnToDock : RobotSignal("ReturnToDock")
}