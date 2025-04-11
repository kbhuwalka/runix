package examples.user_inactivity_detection

import kotlinx.coroutines.flow.MutableStateFlow
import runix.core.RunixScheduler
import runix.dsl.monitor
import runix.primitives.Monitor
import runix.primitives.Signal
import runix.temporal.whenTrue

class UserInactivityApp {
    val userPresent = MutableStateFlow(false)
    val goIdleSignal = object : Signal("userInactiveTooLong") {}

    val monitor: Monitor = monitor("UserIdleMonitor") {
        fireIf(userPresent.whenTrue())
        emits(goIdleSignal)
    }

    fun registerWith(scheduler: RunixScheduler) {
        scheduler.register(monitor)
    }
}
