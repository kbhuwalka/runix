package robot_cleaner.monitors

import robot_cleaner.signals.Signals
import robot_cleaner.state.ObstacleState
import robot_cleaner.state.PowerState
import runix.dsl.monitor
import runix.primitives.Monitor
import runix.temporal.allOf
import runix.temporal.persistedFor
import runix.temporal.wasStableFor
import kotlin.time.Duration.Companion.seconds

object ObstacleMonitors {

    val obstaclePersisted: Monitor = monitor("ObstacleMonitor") {
        fireIf(ObstacleState.obstacleDetected.persistedFor(3.seconds))
        emits(Signals.pauseCleaning)
    }

    val pathClearedMonitor: Monitor = monitor("PathClearMonitor") {
        fireIf(
            allOf(
                ObstacleState.pathIsClear.wasStableFor(2.seconds),
                PowerState.isDocked.wasStableFor(2.seconds)
            )
        )
        emits(Signals.resumeCleaning)
        throttle(5.seconds)
    }

    fun all(): List<Monitor> = listOf(
        obstaclePersisted,
        pathClearedMonitor
    )
}
