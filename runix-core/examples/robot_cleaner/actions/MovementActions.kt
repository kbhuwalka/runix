package robot_cleaner.actions

import kotlinx.coroutines.delay
import robot_cleaner.logger
import robot_cleaner.signals.Signals
import runix.dsl.action
import runix.primitives.ActionError
import runix.primitives.ActionResult
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

object MovementActions {

    val returnToDock = action("ReturnToDock") {
        timeout = 15.seconds
        cancelOn = listOf(Signals.pauseCleaning, Signals.wheelStuck)

        onExecute {
            logger.info("🚗 Navigating to dock...")
            delay(3000)

            if (Random.nextInt(100) < 30) {
                logger.warn("❌ Docking failed")
                return@onExecute ActionResult.Failure(
                    ActionError("dock_failed", "Docking failed due to obstacle"))
            }

            logger.info("✅ Docked successfully")
            ActionResult.Success("Docked")
        }
    }

    val emptyBin = action("EmptyBin") {
        timeout = 10.seconds
        cancelOn = listOf(Signals.pauseCleaning)

        onExecute {
            logger.info("🧺 Emptying bin")
            delay(2000)

            if (Random.nextInt(100) < 25) {
                logger.warn("⚠️ Bin emptying failed")
                return@onExecute ActionResult.Failure(ActionError("bin_error", "Bin mechanism jammed"))
            }

            logger.info("✅ Bin emptied")
            ActionResult.Success("Bin emptied")
        }
    }
}