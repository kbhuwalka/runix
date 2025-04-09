package robot_cleaner.actions

import kotlinx.coroutines.delay
import robot_cleaner.logger
import robot_cleaner.signals.Signals
import robot_cleaner.state.CleaningState
import runix.dsl.action
import runix.primitives.ActionError
import runix.primitives.ActionResult
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

object CleaningActions {

    val startBrush = action("StartBrush") {
        timeout = 3.seconds

        onExecute {
            logger.info("🌀 Starting brush motor")
            delay(500)
            ActionResult.Success("Brush started")
        }
    }

    val stopBrush = action("StopBrush") {
        timeout = 3.seconds

        onExecute {
            logger.info("🛑 Stopping brush motor")
            delay(300)
            ActionResult.Success("Brush stopped")
        }
    }

    val driveCleaningPattern = action("DriveCleaningPattern") {
        timeout = 20.seconds
        cancelOn = listOf(Signals.pauseCleaning, Signals.wheelStuck)

        onExecute {
            logger.info("🚗 Starting cleaning pattern")
            CleaningState.isCleaning.value = true

            repeat(5) {
                delay(1000)
                logger.info("🧽 Cleaning pass $it")

                if (Random.nextInt(100) < 25) {
                    logger.warn("❌ Motor jammed during cleaning")
                    CleaningState.cleaningFailed.value = true
                    CleaningState.isCleaning.value = false
                    return@onExecute ActionResult.Failure(
                        ActionError("cleaning_failed", "Motor jammed during cleaning"))
                }
            }

            CleaningState.isCleaning.value = false
            ActionResult.Success("Completed cleaning pattern")
        }
    }

    val stopMotion = action("StopMotion") {
        timeout = 3.seconds

        onExecute {
            logger.info("🛑 Stopping motion")
            delay(500)
            ActionResult.Success("Motion stopped")
        }
    }

    val stopCleaning = action("StopCleaning") {
        timeout = 5.seconds

        onExecute {
            stopBrush.onExecute(this)
            stopMotion.onExecute(this)
            CleaningState.isCleaning.value = false
            ActionResult.Success("Cleaning stopped")
        }
    }
}