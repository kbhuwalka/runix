package chase_squish

import chase_squish.modules.BrushModule
import chase_squish.scenarios.simulateBrushStress
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import runix.core.RunixScheduler
import runix.primitives.ActionResult

private val logger = LoggerFactory.getLogger("BrushSimulation")

fun main() {
    val scheduler = RunixScheduler()

    // Register BrushModule logic
    BrushModule.allMonitors().forEach { scheduler.register(it) }
    BrushModule.allReactions().forEach { scheduler.register(it) }

    logger.info("✅ BrushModule registered")

    // Attach simulation
    val sim = simulateBrushStress(scheduler)
    sim.attachScheduler(scheduler)

    // Kick off simulation + scheduler
    logger.info("🚀 Starting brush stress simulation")

    runBlocking {
        // Launch loop to retry starting brushes
        scheduler.scope.launch {
            while (true) {
                delay(8000)

                if (!BrushModule.runningState.value) {
                    logger.info("🔁 Attempting to start brushes...")

                    val result = scheduler.runAndWait(BrushModule.Actions.startBrushes)

                    when (result) {
                        is ActionResult.Success -> {
                            logger.info("✅ Brushes started: ${result.message ?: "no message"}")
                        }
                        is ActionResult.Failure -> {
                            logger.warn("❌ Failed to start brushes: ${result.error.message}")
                        }
                        is ActionResult.Cancelled -> {
                            logger.warn("⛔️ Brush start cancelled: ${result.reason}")
                        }
                        else -> {
                            logger.warn("❓ Unexpected action result: $result")
                        }
                    }
                }
            }
        }

        sim.run()
        scheduler.scope.coroutineContext[Job]?.join()
    }

    logger.info("✅ Simulation complete")
}
