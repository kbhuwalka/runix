package examples.stable_condition

import kotlinx.coroutines.flow.MutableStateFlow
import runix.core.RunixScheduler
import runix.dsl.monitor
import runix.primitives.Signal
import runix.temporal.allOf
import runix.temporal.wasStableFor
import runix.temporal.whenTrue
import kotlin.time.Duration.Companion.seconds

class StableConditionApp {
    val isObstacle = MutableStateFlow(false)
    val signal = object : Signal("obstacleDetected") {}

    val monitor = monitor("ObstacleMonitor") {
        fireIf(
            allOf(
                isObstacle.whenTrue(),
                isObstacle.wasStableFor(2.seconds)
            )
        )
        emits(signal)
    }

    fun registerWith(scheduler: RunixScheduler) {
        scheduler.register(monitor)
    }
}
