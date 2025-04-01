package demo.CleanBot

import BotState
import kotlinx.coroutines.delay
import runix.core.logging.primitives.ActionCall
import runix.primitives.*
import runix.primitives.tracing.ExecutionTrace
import kotlin.random.Random

// 1. StartMotor
object StartMotor : Action<Unit>("StartMotor") {
    override suspend fun onExecute(params: Unit, context: ActionContext): ActionResult {
        delay(1000)
        return if (Random.nextDouble() < 0.9) {
            println("⚙️ [${context.trace}] Motor started successfully")
            ActionResult.Success("Motor OK")
        } else {
            println("❌ [${context.trace}] Motor failed to start")
            ActionResult.Failure("Motor failed", recoverable = false)
        }
    }
}

// 2. ActivateSuction
object ActivateSuction : Action<Unit>("ActivateSuction") {
    override suspend fun onExecute(params: Unit, context: ActionContext): ActionResult {
        delay(700)
        return if (Random.nextBoolean()) {
            println("🌀 [${context.trace}] Suction OK")
            ActionResult.Success()
        } else {
            println("❌ [${context.trace}] Suction failed!")
            context.scheduler.fireSignal("SuctionFailure")
            ActionResult.Failure("Suction failed", recoverable = false)
        }
    }
}

// 3. SprayWater
object SprayWater : Action<Unit>("SprayWater") {
    override suspend fun onExecute(params: Unit, context: ActionContext): ActionResult {
        delay(2500)
        return ActionResult.Success("Water sprayed")
    }

    override fun onTimeout(trace: ExecutionTrace) {
        println("⏰ [${trace}] SprayWater timed out")
    }
}

// 4. StartCleaning (high-level action)
object StartCleaning : Action<Unit>("StartCleaning") {
    override suspend fun onExecute(params: Unit, context: ActionContext): ActionResult {
        val trace = context.trace

        val motor = context.scheduler.runNowAndWait(StartMotor, Unit)
        if (motor is ActionResult.Failure) return motor

        val suction = context.scheduler.runNowAndWait(ActivateSuction, Unit)
        if (suction is ActionResult.Failure) return suction

        val spray = context.scheduler.runNowAndWait(SprayWater, Unit)
        if (spray is ActionResult.Failure) return spray

        BotState.isCleaning.value = true
        println("🧹 [${trace}] Cleaning started")
        return ActionResult.Success("Cleaning started")
    }
}

// 5. ReturnToDock
object ReturnToDock : Action<Unit>("ReturnToDock") {
    override suspend fun onExecute(params: Unit, context: ActionContext): ActionResult {
        delay(1000)
        BotState.location.value = "dock"
        BotState.isCleaning.value = false
        return ActionResult.Success("Returning to dock")
    }
}

// 6. RechargeBattery
object RechargeBattery : Action<Unit>("RechargeBattery") {
    override suspend fun onExecute(params: Unit, context: ActionContext): ActionResult {
        println("🔋 Charging...")
        repeat(5) {
            delay(800)
            BotState.batteryLevel.value += 10
            println("⚡ Battery: ${BotState.batteryLevel.value}%")
        }
        return ActionResult.Success("Charged")
    }
}

// 7. AvoidObstacle
object AvoidObstacle : Action<Unit>("AvoidObstacle") {
    override suspend fun onExecute(params: Unit, context: ActionContext): ActionResult {
        println("🧱 Avoiding obstacle...")
        BotState.pathClear.value = false
        delay(1000)
        BotState.obstacleDetected.value = false
        BotState.pathClear.value = true
        return ActionResult.Success("Obstacle avoided")
    }
}

// 8. EmergencyStopAction
object EmergencyStopAction : Action<Unit>("EmergencyStopAction") {
    override suspend fun onExecute(params: Unit, context: ActionContext): ActionResult {
        BotState.isCleaning.value = false
        context.scheduler.fireSignal("EmergencyStop")
        return ActionResult.Success("Emergency stop fired")
    }
}

// 9. Announce
class Announce(private val message: String) : Action<String>("Announce") {
    override suspend fun onExecute(params: String, context: ActionContext): ActionResult {
        println("📣 [${context.trace}] $message")
        return ActionResult.Success("Announced: $message")
    }

    companion object {
        fun withInput(msg: String): ActionCall<String> = ActionCall(Announce(msg), msg)
    }
}