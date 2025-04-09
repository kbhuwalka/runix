package robot_cleaner

import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import robot_cleaner.scenario.simulateRobot
import runix.core.RunixScheduler

val logger = LoggerFactory.getLogger("RobotCleanerMain")

fun main() {
    val scheduler = RunixScheduler()
    App.registerAll(scheduler)

    logger.info("✅ Scheduler registered with all monitors and reactions")

    val sim = simulateRobot()
    sim.attachScheduler(scheduler)

    logger.info("🚀 Simulation running")
    runBlocking {
        sim.run()
        scheduler.scope.coroutineContext[Job]?.join()
    }

    logger.info("✅ Simulation complete and scheduler shut down")
}