package robot_cleaner.actions

import kotlinx.coroutines.delay
import robot_cleaner.logger
import runix.dsl.action
import runix.primitives.ActionResult
import kotlin.time.Duration.Companion.seconds

object UIActions {

    val dimLights = action("DimLights") {
        timeout = 3.seconds

        onExecute {
            logger.info("💡 Dimming lights")
            delay(500)
            ActionResult.Success("Lights dimmed")
        }
    }

    val playChime = action("PlayChime") {
        timeout = 2.seconds

        onExecute {
            logger.info("🔔 Playing chime")
            delay(750)
            ActionResult.Success("Chime played")
        }
    }
}
