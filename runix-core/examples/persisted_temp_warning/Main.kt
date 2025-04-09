package examples.persisted_temp_warning

import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import runix.core.RunixScheduler

private val logger = LoggerFactory.getLogger("PersistedTempExample")

fun main() {
    val app = PersistedTempApp()
    val scheduler = RunixScheduler()
    app.registerWith(scheduler)
    logger.info("✅ Scheduler registered")

    val sim = simulateScenario(app)
    sim.attachScheduler(scheduler)
    logger.info("🚀 Simulation running")

    runBlocking {
        sim.run()
        scheduler.scope.coroutineContext[Job]?.join()
    }

    logger.info("✅ Simulation complete")
}