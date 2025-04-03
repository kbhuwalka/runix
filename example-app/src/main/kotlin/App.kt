import kotlinx.coroutines.delay
import runix.primitives.RunixScheduler
import runix.tools.TimelineViewer
import runix.tracing.FileTraceLogger
import runix.tracing.tools.TraceVisualizer
import java.io.File

suspend fun main() {
    println("🚀 Booting WarehouseBot Brain...")
    val logger = FileTraceLogger(File("logs"))
    val scheduler = RunixScheduler(traceLogger = logger)

    val handles = listOf(
        scheduler.register(Monitors.LowSpeedDuringCalibration()),
        scheduler.register(Monitors.OverheatMonitor()),
        scheduler.register(Monitors.BatteryMonitor()),
        scheduler.register(Reactions.FailCalibration),
        scheduler.register(Reactions.SucceedCalibration),
        scheduler.register(Reactions.HandleOverheat),
        scheduler.register(Reactions.HandleBatteryLow),
        scheduler.register(Reactions.ResumeDelivery)
    )

    scheduler.start()

    println("🧠 SYSTEM BOOTING...")
    BotState.batteryLevel.value = 100
    BotState.temperature.value = 60.0
    BotState.currentLocation.value = "dock"
    BotState.isCalibrating.value = false
    BotState.isMoving.value = false

    delay(1000)

    println("\n🎯 [STEP 1] STARTING CALIBRATION")
    scheduler.schedule(CalibrateCamera())

    // Simulate low speed for monitor to trigger
    delay(1500)
    println("🐢 Artificially slowing down to trigger low-speed monitor...")
    BotState.currentSpeed.value = 3.0
    delay(6000)
    BotState.currentSpeed.value = 0.0 // simulate stop

    delay(1000)

    println("\n📦 [STEP 2] BEGINNING PACKAGE DELIVERY")
    scheduler.schedule(DeliverPackages())

    delay(2000)
    println("🔥 [STEP 3] Simulating OVERHEAT condition")
    BotState.temperature.value = 90.0
    delay(11000) // long enough to trigger monitor

    println("❄️ Cooling system recovers...")
    BotState.temperature.value = 70.0
    delay(2000)

    println("\n🔋 [STEP 4] Simulating LOW BATTERY condition")
    BotState.batteryLevel.value = 20
    delay(16000) // long enough to trigger BatteryLow

    println("\n⚡ [STEP 5] Battery charges...")
    delay(6000) // enough for ChargeBattery + ResumeDelivery

    println("\n📦 [STEP 6] Continuing delivery...")
    scheduler.schedule(DeliverPackages())
    delay(6000)

    println("\n✅ [STEP 7] Simulation complete.")
    handles.forEach { it.dispose() }

    val logFile = logger.getLogFile()
    val outputDir = File("logs/diagrams")
    TraceVisualizer.generateFrom(logFile, outputDir)

    println("✅ Mermaid diagrams written to ${outputDir.absolutePath}")
    TimelineViewer.printTimeline(logFile)
}