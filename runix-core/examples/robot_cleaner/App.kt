package robot_cleaner

import robot_cleaner.monitors.*
import robot_cleaner.reactions.*
import runix.core.RunixScheduler

object App {
    fun registerAll(scheduler: RunixScheduler) {
        // Register monitors
        BatteryMonitors.all().forEach { scheduler.register(it) }
        CleaningMonitors.all().forEach { scheduler.register(it) }
        ObstacleMonitors.all().forEach { scheduler.register(it) }
        UserMonitors.all().forEach { scheduler.register(it) }

        // Register reactions
        CleaningReactions.all().forEach { scheduler.register(it) }
        PowerReactions.all().forEach { scheduler.register(it) }
        UIReactions.all().forEach { scheduler.register(it) }
    }
}