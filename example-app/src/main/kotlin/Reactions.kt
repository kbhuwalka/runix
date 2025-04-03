import runix.dsl.reaction
import runix.primitives.Reaction

object Reactions {

    val FailCalibration = reaction("FailCalibration") {
        on(RobotSignal.CalibrationFailed)
        run {
            BotState.isCalibrating.value = false
            println("❌ Calibration failed.")
        }
    }

    val SucceedCalibration = reaction("SucceedCalibration") {
        on(RobotSignal.CalibrationSucceeded)
        run {
            BotState.isCalibrating.value = false
            println("✅ Calibration succeeded.")
        }
    }

    val HandleOverheat = reaction("HandleOverheat") {
        on(RobotSignal.OverheatWarning)
        run { ctx ->
            BotState.currentLocation.value = "dock"
            ctx.schedule(Announce("Overheat warning. I am too hot!!"))
        }
    }

    val HandleBatteryLow = reaction("HandleBatteryLow") {
        on(RobotSignal.BatteryLow)
        run { ctx ->
            println("Reaction triggered for battery level: ${BotState.batteryLevel.value}")
            ctx.schedule(ChargeBattery())
        }
    }

    val ResumeDelivery = reaction("ResumeDelivery") {
        on(RobotSignal.BatteryCharged)
        run { ctx ->
            ctx.schedule(DeliverPackages())
        }
    }
}