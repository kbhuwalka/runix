package demo.CleanBot

import BotState
import runix.primitives.Reaction
import runix.primitives.tracing.child

object Reactions {

    // 1. Start cleaning when user requests it
    val StartOnCommand = Reaction(
        name = "StartOnCommand",
        dependsOn = listOf(BotState.startRequested),
        condition = { BotState.startRequested.value },
        onFired = { ctx ->
            val announcement = "✅ Docked successfully"
            ctx.scheduler.schedule(Announce(announcement), announcement, ctx.trace.child("Announce"))
            ctx.scheduler.runNow(StartCleaning, Unit)
        }
    )

    // 2. Abort if battery is low during cleaning
    val LowBatteryAbort = Reaction(
        name = "LowBatteryAbort",
        dependsOn = listOf(BotState.batteryLevel, BotState.isCleaning),
        condition = {
            BotState.batteryLevel.value < 15 && BotState.isCleaning.value
        },
        onFired = { ctx ->
            val announcement = "🔋 Battery low, returning"
            ctx.scheduler.schedule(Announce(announcement), announcement, ctx.trace.child("Announce"))
            ctx.scheduler.runNow(ReturnToDock, Unit)
        }
    )

    // 3. Start charging when docked
    val DockedAndCharging = Reaction(
        name = "DockedAndCharging",
        dependsOn = listOf(BotState.location),
        condition = { BotState.location.value == "dock" },
        onFired = { ctx ->
            ctx.scheduler.runNow(RechargeBattery, Unit)
        }
    )

    // 4. Resume cleaning after full battery
    val ResumeAfterRecharge = Reaction(
        name = "ResumeAfterRecharge",
        dependsOn = listOf(BotState.batteryLevel, BotState.location),
        condition = {
            BotState.batteryLevel.value >= 80 && BotState.location.value == "dock"
        },
        onFired = { ctx ->
            val announcement = "🔋 Resuming after full charge"
            ctx.scheduler.schedule(Announce(announcement), announcement, ctx.trace.child("Announce"))
            ctx.scheduler.runNow(StartCleaning, Unit)
        }
    )

    // 5. Obstacle detected
    val ObstacleDetected = Reaction(
        name = "ObstacleDetected",
        dependsOn = listOf(BotState.obstacleDetected),
        condition = { BotState.obstacleDetected.value },
        onFired = { ctx ->

            val announcement = "🚧 Obstacle in path"
            ctx.scheduler.schedule(Announce(announcement), announcement, ctx.trace.child("Announce"))
            ctx.scheduler.runNow(AvoidObstacle, Unit)
        }
    )

    // 6. Suction failure fallback
    val CleaningFailed = Reaction(
        name = "CleaningFailed",
        dependsOn = emptyList(),
        signalNames = listOf("SuctionFailure"),
        condition = { true },
        onFired = { ctx ->

            val announcement = "❌ Suction failure, aborting"
            ctx.scheduler.schedule(Announce(announcement), announcement, ctx.trace.child("Announce"))
            ctx.scheduler.runNow(ReturnToDock, Unit, ctx.trace.child("ReturnToDock"))
        }
    )

    // 7. Emergency stop
    val EmergencyTriggered = Reaction(
        name = "EmergencyTriggered",
        dependsOn = emptyList(),
        signalNames = listOf("EmergencyStop"),
        condition = { true },
        onFired = { ctx ->

            val announcement = "🛑 Emergency stop"
            ctx.scheduler.runNow(Announce(announcement), announcement, ctx.trace.child("Announce"))
            ctx.scheduler.runNow(ReturnToDock, Unit)
        }
    )

    // 8. Alert user for any critical condition
    val AlertUserOnError = Reaction(
        name = "AlertUserOnError",
        dependsOn = emptyList(),
        signalNames = listOf("SuctionFailure", "EmergencyStop"),
        condition = { true },
        onFired = { ctx ->
            val announcement = "⚠️ Critical event triggered"
            ctx.scheduler.schedule(Announce(announcement), announcement, ctx.trace.child("Announce"))
        }
    )
}