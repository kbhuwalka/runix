import kotlinx.coroutines.delay
import runix.primitives.Action
import runix.primitives.ActionContext
import runix.primitives.ActionResult

class Announce(private val message: String) : Action("Announce") {
    override suspend fun onExecute(context: ActionContext): ActionResult {
        println("📢 Announcement: $message")
        return ActionResult.Success()
    }
}

class StartMotors : Action("StartMotors") {
    override suspend fun onExecute(context: ActionContext): ActionResult {
        delay(500)
        val failed = Math.random() < 0.3
        if (failed) {
            BotState.currentSpeed.value = 2.0
            return ActionResult.Failure("Motor failed")
        }
        BotState.currentSpeed.value = 6.0
        BotState.isMoving.value = true
        return ActionResult.Success()
    }
}

class CalibrateCamera : Action("CalibrateCamera") {
    override suspend fun onExecute(context: ActionContext): ActionResult {
        BotState.isCalibrating.value = true
        context.fireSignal(RobotSignal.CalibrationStarted)
        delay(1000)
        val motors = context.runAndWait(StartMotors())
        delay(3000)
        return if (motors is ActionResult.Success) {
            context.fireSignal(RobotSignal.CalibrationSucceeded)
            ActionResult.Success()
        } else {
            context.fireSignal(RobotSignal.CalibrationFailed)
            ActionResult.Failure("Calibration failed")
        }
    }
}

class DeliverPackage : Action("DeliverPackage") {
    override suspend fun onExecute(context: ActionContext): ActionResult {
        delay(500)
        val success = Math.random() < 0.8
        if (success) {
            BotState.deliveriesCompleted.value += 1
            context.fireSignal(RobotSignal.DeliverySuccess)
            return ActionResult.Success()
        }
        return ActionResult.Failure("Delivery failed")
    }
}

class DeliverPackages : Action("DeliverPackages") {
    override suspend fun onExecute(context: ActionContext): ActionResult {
        repeat(5) {
            val result = context.runAndWait(DeliverPackage())
            delay(1000)
            BotState.temperature.value += (-2..5).random()
            BotState.batteryLevel.value -= (2..4).random()
            if (result is ActionResult.Failure) return result
        }
        return ActionResult.Success()
    }
}

class ChargeBattery : Action("ChargeBattery") {
    override suspend fun onExecute(context: ActionContext): ActionResult {
        BotState.isCharging.value = true
        repeat(5) {
            delay(400)
            BotState.batteryLevel.value += 10
        }
        BotState.isCharging.value = false
        context.fireSignal(RobotSignal.BatteryCharged)
        return ActionResult.Success("Battery charged")
    }
}