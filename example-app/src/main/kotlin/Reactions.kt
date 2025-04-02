package demo.CleanBot

import BotState
import DropFailed
import DropSucceeded
import FallbackNavigationFailed
import GripperFailed
import MotorFailed
import NavigationFailed
import StartDelivery
import runix.primitives.Reaction

object Reactions {

    val StartOnCommand = Reaction(
        name = "Reaction(StartOnCommand)",
        signalNames = listOf(StartDelivery),
        dependsOn = emptyList(),
        condition = { true },
        onFired = { ctx ->
            ctx.runAll(
                Announce("🚀 Starting delivery"),
                StartDeliverySequence
            )
        }
    )

    val DropFailedHandler = Reaction(
        name = "Reaction(DropFailedHandler)",
        signalNames = listOf(DropFailed),
        dependsOn = emptyList(),
        condition = { true },
        onFired = { ctx ->
            ctx.runAll(
                Announce("❌ Drop failed. Retrying..."),
                DropPackage
            )
        }
    )

    val RetryDropEscalation = Reaction(
        name = "Reaction(RetryEscalation)",
        signalNames = listOf(DropFailed),
        dependsOn = listOf(BotState.dropSuccessful),
        condition = { !BotState.dropSuccessful.value },
        onFired = { ctx ->
            ctx.runAll(
                Announce("⚠️ Retrying failed. Going to fallback."),
                NavigateToFallback,
                DropPackage
            )
        }
    )

    val MotorFailureHandler = Reaction(
        name = "Reaction(MotorFailure)",
        signalNames = listOf(MotorFailed),
        dependsOn = listOf(),
        condition = { true },
        onFired = { ctx ->
            ctx.runAll(
                Announce("🛑 Motor failed. Returning to base."),
                ReturnToDock
            )
        }
    )

    val GripperFailureHandler = Reaction(
        name = "Reaction(GripperFailure)",
        signalNames = listOf(GripperFailed),
        dependsOn = listOf(),
        condition = { true },
        onFired = { ctx ->
            ctx.runAll(
                Announce("🔧 Gripper jammed. Attempting restart."),
                ActivateGripper
            )
        }
    )

    val DropSuccessWrapUp = Reaction(
        name = "Reaction(WrapUp)",
        signalNames = listOf(DropSucceeded),
        dependsOn = listOf(),
        condition = { true },
        onFired = { ctx ->
            ctx.runAll(
                Announce("✅ Package delivered. Returning to dock."),
                ReturnToDock
            )
        }
    )

    val BatteryLowInterruption = Reaction(
        name = "Reaction(BatteryLowHandler)",
        dependsOn = listOf(BotState.batteryLevel),
        condition = { BotState.batteryLevel.value < 20 },
        onFired = { ctx ->
            ctx.runAll(
                Announce("🔋 Battery low. Docking to charge."),
                ReturnToDock,
                ChargeBattery
            )
        }
    )

    val EscalateAfterDropFails = Reaction(
        name = "Reaction(EscalateAfterDropFails)",
        dependsOn = listOf(BotState.dropAttempts),
        condition = { BotState.dropAttempts.value >= 3 && !BotState.dropSuccessful.value },
        onFired = { ctx ->
            ctx.runAll(
                Announce("🚨 Drop failed 3 times. Escalating."),
                ReturnToDock
            )
        }
    )

    val FallbackNavFailure = Reaction(
        name = "Reaction(FallbackNavFailure)",
        signalNames = listOf(FallbackNavigationFailed),
        dependsOn = listOf(BotState.currentZone),
        condition = { BotState.currentZone.value == "fallback" },
        onFired = { ctx ->
            ctx.runAll(
                Announce("🧯 Fallback nav failed. Returning to dock."),
                ReturnToDock
            )
        }
    )

    val HandleNavigationFailure = Reaction(
        name = "Reaction(HandleNavigationFailure)",
        signalNames = listOf(NavigationFailed),
        dependsOn = listOf(BotState.currentZone),
        condition = { BotState.currentZone.value != "fallback" },
        onFired = { ctx ->
            ctx.runAll(
                Announce("⚠️ Navigation failed. Retrying main drop zone."),
                NavigateToDropZone
            )
        }
    )

}