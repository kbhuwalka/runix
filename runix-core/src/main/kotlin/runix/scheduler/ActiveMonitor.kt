package runix.scheduler

import runix.temporal.CompiledMonitor
import runix.primitives.Monitor

data class ActiveMonitor(
    val monitor: Monitor,
    val compiled: CompiledMonitor
)