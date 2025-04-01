package demo.CleanBot

import BotState
import kotlinx.coroutines.*
import runix.primitives.RunixScheduler
import runix.tools.TimelineViewer
import runix.tracing.FileTraceLogger
import runix.tracing.tools.TraceVisualizer
import java.io.File

fun main() = runBlocking {
    println("🚀 Booting CleanBot X1...")
    val logger = FileTraceLogger(File("logs"))
    val scheduler = RunixScheduler(traceLogger = logger)
    scheduler.start()

    val handles = listOf(
        scheduler.register(Reactions.StartOnCommand),
        scheduler.register(Reactions.LowBatteryAbort),
        scheduler.register(Reactions.DockedAndCharging),
        scheduler.register(Reactions.ResumeAfterRecharge),
        scheduler.register(Reactions.ObstacleDetected),
        scheduler.register(Reactions.CleaningFailed),
        scheduler.register(Reactions.EmergencyTriggered),
        scheduler.register(Reactions.AlertUserOnError),
    )

    delay(1000)

    println("\n🧠 [User] Starting cleaning")
    BotState.startRequested.value = true

    delay(2000)

    println("\n📦 [System] Cleaning in progress... Updating state...")
    BotState.location.value = "room"
    BotState.batteryLevel.value = 95

    delay(1500)

    println("\n💧 [Sensor] Water tank now empty")
    BotState.waterTankFull.value = false

    delay(3000)

    println("\n🧼 [CleanBot] Docked for refill")
    BotState.location.value = "dock"

    delay(2000)

    println("\n💧 [User] Refilling water tank manually...")
    BotState.waterTankFull.value = true

    delay(2000)

    println("\n🧱 [Sensor] Obstacle detected (furniture)")
    BotState.obstacleDetected.value = true

    delay(2500)

    println("\n🔋 [System] Battery draining...")
    BotState.batteryLevel.value = 40
    delay(1000)
    BotState.batteryLevel.value = 25
    delay(1000)
    BotState.batteryLevel.value = 14

    delay(3000)

    println("\n💤 [CleanBot] Docked and charging...")
    BotState.location.value = "dock"

    delay(500)

    println("\n🔌 [Charger] Boosting battery...")
    BotState.batteryLevel.value = 30
    delay(1000)
    BotState.batteryLevel.value = 50
    delay(1000)
    BotState.batteryLevel.value = 65
    delay(1000)
    BotState.batteryLevel.value = 80

    delay(3000)

    println("\n🧪 [CleanBot] Resumed cleaning post-charge")
    BotState.location.value = "room"
    BotState.startRequested.value = true

    delay(2000)

    println("\n⚠️ [System] Suction failure simulated")
    scheduler.runNow(ActivateSuction, Unit)

    delay(3000)

    println("\n🆘 [User] Emergency stop fired!")
    scheduler.runNow(EmergencyStopAction, Unit)

    delay(3000)

    println("\n🧼 [User] Restarting cleaning manually...")
    BotState.batteryLevel.value = 85
    BotState.location.value = "room"
    BotState.startRequested.value = true

    delay(2500)

    println("\n🧱 [Sensor] Obstacle detected again (cable)")
    BotState.obstacleDetected.value = true

    delay(2500)

    println("\n✅ [Sensor] Path now clear again")
    BotState.pathClear.value = true

    delay(2000)

    println("\n🕒 [System] Cleaning complete simulated")
    scheduler.fireSignal("CleaningComplete")

    delay(3000)

    println("\n✅ Simulation complete. Shutting down CleanBot X1.")
    handles.forEach { it.dispose() }

    val logFile = logger.getLogFile()
    val outputDir = File("logs/diagrams")
    TraceVisualizer.generateFrom(logFile, outputDir)

    println("✅ Mermaid diagrams written to ${outputDir.absolutePath}")

    TimelineViewer.printTimeline(logFile)
}