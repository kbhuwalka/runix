package demo.CleanBot

import BotState
import kotlinx.coroutines.delay
import runix.primitives.*
import kotlin.random.Random

object StartMotor : Action("StartMotor") {
    override suspend fun onExecute(context: ActionContext): ActionResult {
        delay(500)
        return if (Random.nextDouble() < 0.9) {
            ActionResult.Success("Motor started")
        } else {
            context.fireSignal("MotorFailure")
            ActionResult.Failure("Motor failed", recoverable = false)
        }
    }
}

object ActivateGripper : Action("ActivateGripper") {
    override suspend fun onExecute(context: ActionContext): ActionResult {
        delay(400)
        return if (Random.nextBoolean()) {
            ActionResult.Success("Gripper activated")
        } else {
            context.fireSignal("GripperFailure")
            ActionResult.Failure("Gripper jammed", recoverable = true)
        }
    }
}

object NavigateToDropZone : Action("NavigateToDropZone") {
    override suspend fun onExecute(context: ActionContext): ActionResult {
        delay(800)
        return if (Random.nextDouble() < 0.85) {
            BotState.currentZone.value = "drop"
            ActionResult.Success("Reached drop zone")
        } else {
            context.fireSignal("NavigationFailed")
            ActionResult.Failure("Couldn't reach drop zone")
        }
    }
}

object DropPackage : Action("DropPackage") {
    override suspend fun onExecute(context: ActionContext): ActionResult {
        delay(600)
        return if (Random.nextDouble() < 0.75) {
            BotState.dropSuccessful.value = true
            BotState.dropAttempts.value += 1
            context.fireSignal("DropSuccess")
            ActionResult.Success("Dropped package")
        } else {
            context.fireSignal("DropFailed")
            ActionResult.Failure("Drop failed", recoverable = true)
        }
    }
}

object NavigateToFallback : Action("NavigateToFallback") {
    override suspend fun onExecute(context: ActionContext): ActionResult {
        delay(1000)
        BotState.currentZone.value = "fallback"
        return ActionResult.Success("Fallback reached")
    }
}

object ReturnToDock : Action("ReturnToDock") {
    override suspend fun onExecute(context: ActionContext): ActionResult {
        delay(700)
        return ActionResult.Success("Returned to dock")
    }
}

object ChargeBattery : Action("ChargeBattery") {
    override suspend fun onExecute(context: ActionContext): ActionResult {
        repeat(5) {
            delay(400)
            BotState.batteryLevel.value += 10
        }
        return ActionResult.Success("Battery charged")
    }
}

class Announce(private val message: String) : Action("Announce($message)") {

    override suspend fun onExecute(context: ActionContext): ActionResult {
        println("📣 $message")
        return ActionResult.Success()
    }
}

object StartDeliverySequence : Action("StartDeliverySequence") {
    override suspend fun onExecute(context: ActionContext): ActionResult {
        val motor = context.runChildAndWait(StartMotor)
        if (motor is ActionResult.Failure) return motor

        val nav = context.runChildAndWait(NavigateToDropZone)
        if (nav is ActionResult.Failure) return nav

        val drop = context.runChildAndWait(DropPackage)
        return drop
    }
}