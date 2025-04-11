package examples.stable_condition

import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import runix.core.RunixScheduler

private val logger = LoggerFactory.getLogger("StableConditionExample")

fun main() {
    val app = StableConditionApp()
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

    logger.info("✅ Simulation finished")
}
