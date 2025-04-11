package examples.basic_monitor

import kotlinx.coroutines.flow.MutableStateFlow
import runix.core.RunixScheduler
import runix.dsl.monitor
import runix.primitives.Signal
import runix.temporal.whenTrue

class DirtDetectorApp {
    val isDirty = MutableStateFlow(false)
    val startCleaning = object : Signal("startCleaning") {}

    val monitor = monitor("DirtDetector") {
        fireIf(isDirty.whenTrue())
        emits(startCleaning)
    }

    fun registerWith(scheduler: RunixScheduler) {
        scheduler.register(monitor)
    }
}
