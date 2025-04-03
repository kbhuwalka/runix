import runix.primitives.Reaction

object Reactions {
    val FailCalibration = Reaction(
        name = "FailCalibration",
        signalNames = listOf(RobotSignal.CalibrationFailed),
        dependsOn = emptyList(),
        condition = { true },
        onFired = { ctx ->
            BotState.isCalibrating.value = false
            println("❌ Calibration failed.")
        }
    )

    val SucceedCalibration = Reaction(
        name = "SucceedCalibration",
        signalNames = listOf(RobotSignal.CalibrationSucceeded),
        dependsOn = emptyList(),
        condition = { true },
        onFired = { ctx ->
            BotState.isCalibrating.value = false
            println("✅ Calibration succeeded.")
        }
    )

    val HandleOverheat = Reaction(
        name = "HandleOverheat",
        signalNames = listOf(RobotSignal.OverheatWarning),
        dependsOn = emptyList(),
        condition = { true },
        onFired = { ctx ->
            BotState.currentLocation.value = "dock"
            ctx.scheduleChild(Announce("Overheat warning. Returning to dock."))
        }
    )

    val HandleBatteryLow = Reaction(
        name = "HandleBatteryLow",
        signalNames = listOf(RobotSignal.BatteryLow),
        dependsOn = emptyList(),
        condition = { true },
        onFired = { ctx -> ctx.scheduleChild(ChargeBattery()) }
    )

    val ResumeDelivery = Reaction(
        name = "ResumeDelivery",
        signalNames = listOf(RobotSignal.BatteryCharged),
        dependsOn = emptyList(),
        condition = { true },
        onFired = { ctx -> ctx.scheduleChild(DeliverPackages()) }
    )
}