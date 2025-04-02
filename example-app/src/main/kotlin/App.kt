package demo.CleanBot

import BotState
import DropFailed
import MotorFailed
import StartDelivery
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import runix.primitives.RunixScheduler
import runix.tools.TimelineViewer
import runix.tracing.FileTraceLogger
import runix.tracing.tools.TraceVisualizer
import java.io.File

fun main() = runBlocking {
    println("🚀 Booting WarehouseBot Brain...")
    val logger = FileTraceLogger(File("logs"))
    val scheduler = RunixScheduler(traceLogger = logger)
    scheduler.start()

    val handles = listOf(
        scheduler.register(Reactions.StartOnCommand),
        scheduler.register(Reactions.DropFailedHandler),
        scheduler.register(Reactions.RetryDropEscalation),
        scheduler.register(Reactions.MotorFailureHandler),
        scheduler.register(Reactions.GripperFailureHandler),
        scheduler.register(Reactions.DropSuccessWrapUp),
        scheduler.register(Reactions.BatteryLowInterruption),
        scheduler.register(Reactions.FallbackNavFailure),
        scheduler.register(Reactions.EscalateAfterDropFails),
        scheduler.register(Reactions.HandleNavigationFailure)
    )

    delay(1000)

    println("\n🧠 [User] Starting delivery")
    scheduler.fireSignal(StartDelivery)

    delay(2000)

    println("\n⚡ [System] Simulating gradual battery drain")
    BotState.batteryLevel.value = 50
    delay(1000)
    BotState.batteryLevel.value = 30
    delay(1000)
    BotState.batteryLevel.value = 18

    delay(5000)

    println("\n🎯 [Sim] Faking drop failure and retry")
    scheduler.fireSignal(DropFailed)

    delay(4000)

    println("\n🔧 [Sim] Faking motor failure (should cancel delivery)")
    scheduler.fireSignal(MotorFailed)

    delay(3000)

    println("\n🧼 [Reset] Resuming delivery after fix")
    BotState.batteryLevel.value = 85
    scheduler.fireSignal(StartDelivery)

    delay(5000)

    println("\n✅ Simulation complete. Shutting down WarehouseBot.")
    handles.forEach { it.dispose() }

    val logFile = logger.getLogFile()
    val outputDir = File("logs/diagrams")
    TraceVisualizer.generateFrom(logFile, outputDir)

    println("✅ Mermaid diagrams written to ${outputDir.absolutePath}")
    TimelineViewer.printTimeline(logFile)
}