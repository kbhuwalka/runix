package robot_cleaner.monitors

import robot_cleaner.signals.Signals
import robot_cleaner.state.CleaningState
import runix.dsl.monitor
import runix.primitives.Monitor
import runix.temporal.persistedFor
import kotlin.time.Duration.Companion.seconds

object CleaningMonitors {

    val binFullMonitor: Monitor = monitor("BinFullMonitor") {
        fireIf(CleaningState.isBinFull.persistedFor(10.seconds))
        emits(Signals.emptyBin)
        throttle(60.seconds)
    }

    fun all(): List<Monitor> = listOf(
        binFullMonitor
    )
}
