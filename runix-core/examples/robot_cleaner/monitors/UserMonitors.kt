package robot_cleaner.monitors

import robot_cleaner.signals.Signals
import robot_cleaner.state.UserState
import runix.dsl.monitor
import runix.primitives.Monitor
import runix.temporal.wasSilentFor
import kotlin.time.Duration.Companion.seconds

object UserMonitors {

    val idleMonitor: Monitor = monitor("UserIdleMonitor") {
        dependsOn(UserState.userPresent)
        fireIf(UserState.userPresent.wasSilentFor(60.seconds, "user-inactive"))
        emits(Signals.dimLights)
    }

    fun all(): List<Monitor> = listOf(
        idleMonitor
    )
}