package examples.stable_condition

import kotlinx.coroutines.flow.MutableStateFlow
import runix.core.RunixScheduler
import runix.dsl.monitor
import runix.primitives.Signal
import runix.temporal.allOf
import runix.temporal.asCondition
import runix.temporal.wasStableFor
import kotlin.time.Duration.Companion.seconds

class StableConditionApp {
    val isObstacle = MutableStateFlow(false)
    val signal = object : Signal("obstacleDetected") {}

    val monitor = monitor("ObstacleMonitor") {
        dependsOn(isObstacle)
        fireIf(allOf(
            isObstacle.asCondition("is-obstacle"),
            isObstacle.wasStableFor(2.seconds, "obstacle-stable")
        ))
        emits(signal)
    }

    fun registerWith(scheduler: RunixScheduler) {
        scheduler.register(monitor)
    }
}