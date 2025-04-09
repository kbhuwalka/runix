package examples.basic_monitor

import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import runix.core.RunixScheduler

private val logger = LoggerFactory.getLogger("DirtDetectorMain")

fun main() {
    val app = DirtDetectorApp()
    val scheduler = RunixScheduler()
    app.registerWith(scheduler)
    logger.info("✅ Scheduler registered")

    val sim = simulateScenario(app)
    sim.attachScheduler(scheduler)
    logger.info("🚀 Simulation starting")

    runBlocking {
        sim.run()
        scheduler.scope.coroutineContext[Job]?.join()
    }

    logger.info("✅ Simulation complete and scheduler idle")
}