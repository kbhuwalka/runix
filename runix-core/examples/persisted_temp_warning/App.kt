package examples.persisted_temp_warning

import kotlinx.coroutines.flow.MutableStateFlow
import runix.core.RunixScheduler
import runix.dsl.derivedSignal
import runix.dsl.monitor
import runix.primitives.Signal
import runix.temporal.persistedFor
import kotlin.time.Duration.Companion.seconds

class PersistedTempApp {
    val temperature = MutableStateFlow(75f)

    // Derived signal — isHot becomes true when temp > 85
    private val isHot = derivedSignal(temperature) { it > 85f }

    val signal = object : Signal("overheatWarning") {}

    private val monitor= monitor("PersistedOverheatMonitor") {
        dependsOn(isHot)
        fireIf(isHot.persistedFor(5.seconds, "is-hot-persisted"))
        emits(signal)
    }

    fun registerWith(scheduler: RunixScheduler) {
        scheduler.register(monitor)
    }
}