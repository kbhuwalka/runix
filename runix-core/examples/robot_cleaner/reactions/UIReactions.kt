package robot_cleaner.reactions

import robot_cleaner.actions.UIActions
import robot_cleaner.logger
import robot_cleaner.signals.Signals
import runix.dsl.reaction
import runix.primitives.Reaction

object UIReactions {

    val dimLights = reaction("DimLights") {
        on(Signals.dimLights)
        run { context ->
            logger.info("💡 Dimming lights due to inactivity")
            context.runAndWait(UIActions.dimLights)
        }
    }

    val playChimeOnStart = reaction("PlayChimeOnStart") {
        on(Signals.startCleaning)
        run { context ->
            logger.info("🔔 Playing start chime")
            context.runAndWait(UIActions.playChime)
        }
    }

    fun all(): List<Reaction> = listOf(
        dimLights,
        playChimeOnStart
    )
}