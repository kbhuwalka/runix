package robot_cleaner.reactions

import robot_cleaner.actions.MovementActions
import robot_cleaner.logger
import robot_cleaner.signals.Signals
import robot_cleaner.state.PowerState
import runix.dsl.reaction
import runix.primitives.Reaction

object PowerReactions {

    val returnToBase = reaction("ReturnToBase") {
        on(Signals.returnToBase)
        run { context ->
            if (!PowerState.isCharging.value && !PowerState.isDocked.value) {
                logger.info("🔋 Battery low — returning to base")
                context.runAndWait(MovementActions.returnToDock)
            } else {
                logger.info("⚡ Already docked or charging — skipping return")
            }
        }
    }

    val emptyBin = reaction("EmptyBin") {
        on(Signals.emptyBin)
        run { context ->
            logger.info("🧺 Bin full — initiating emptying process")
            context.runAndWait(MovementActions.emptyBin)
        }
    }

    fun all(): List<Reaction> = listOf(
        returnToBase,
        emptyBin
    )
}