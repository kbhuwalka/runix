package robot_cleaner.reactions

import robot_cleaner.actions.CleaningActions
import robot_cleaner.logger
import robot_cleaner.signals.Signals
import runix.dsl.reaction
import runix.primitives.ActionResult
import runix.primitives.Reaction

object CleaningReactions {

    val startCleaning = reaction("StartCleaning") {
        on(Signals.startCleaning)
        run { context ->
            logger.info("🧽 Starting cleaning session")

            val brushResult = context.runAndWait(CleaningActions.startBrush)
            if (brushResult is ActionResult.Failure) return@run

            val driveResult = context.runAndWait(CleaningActions.driveCleaningPattern)
            if (driveResult is ActionResult.Failure) {
                context.fire(Signals.cleaningFailed)
            }
        }
    }

    val stopCleaning = reaction("StopCleaning") {
        on(Signals.stopCleaning)
        run { context ->
            logger.info("🛑 Stopping cleaning session")
            context.runAndWait(CleaningActions.stopCleaning)
        }
    }

    val pauseCleaning = reaction("PauseCleaning") {
        on(Signals.pauseCleaning)
        run { context ->
            logger.info("⏸ Pausing cleaning due to external signal")
            context.runAndWait(CleaningActions.stopCleaning)
        }
    }

    val resumeCleaning = reaction("ResumeCleaning") {
        on(Signals.resumeCleaning)
        run { context ->
            logger.info("▶️ Resuming cleaning session")
            context.runAndWait(CleaningActions.driveCleaningPattern)
        }
    }

    val handleCleaningFailure = reaction("HandleCleaningFailure") {
        on(Signals.cleaningFailed)
        run { context ->
            logger.info("❌ Cleaning failed — handling recovery")
            context.runAndWait(CleaningActions.stopCleaning)
        }
    }

    fun all(): List<Reaction> = listOf(
        startCleaning,
        stopCleaning,
        pauseCleaning,
        resumeCleaning,
        handleCleaningFailure
    )
}