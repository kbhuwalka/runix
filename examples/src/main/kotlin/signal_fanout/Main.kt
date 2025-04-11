package examples.signal_fanout

import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import runix.core.RunixScheduler

val logger = LoggerFactory.getLogger("SignalFanoutExample")

fun main() {
    val app = SignalFanoutApp()
    val scheduler = RunixScheduler()

    app.registerWith(scheduler)
    logger.info("✅ Scheduler registered and all monitors/reactions wired")

    val sim = simulateScenario(app)
    sim.attachScheduler(scheduler)
    logger.info("🚀 Simulation starting")

    runBlocking {
        sim.run()
        scheduler.scope.coroutineContext[Job]?.join()
    }

    logger.info("✅ Simulation complete and scheduler idle")
}
