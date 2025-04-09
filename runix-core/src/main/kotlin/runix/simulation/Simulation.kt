package runix.simulation

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import runix.core.RunixScheduler

class Simulation {
    private val clock = SimClock()
    private val builder = TimelineBuilder()
    private var scheduler: RunixScheduler? = null

    fun <T> schedule(
        flow: MutableStateFlow<T>,
        name: String,
        block: SignalSchedule<T>.() -> Unit
    ) {
        builder.forSignal(flow, name, block)
    }

    fun attachScheduler(scheduler: RunixScheduler) {
        this.scheduler = scheduler
    }

    suspend fun run(realtime: Boolean = true) {
        val allChanges = builder.allChanges().sortedBy { it.time }
        val startWallClock = System.currentTimeMillis()

        for (change in allChanges) {
            val simTargetTime = change.time
            val delayMs = simTargetTime - clock.now

            if (realtime && delayMs > 0) {
                val expectedWallTime = startWallClock + simTargetTime
                val currentWallTime = System.currentTimeMillis()
                val waitTime = expectedWallTime - currentWallTime
                if (waitTime > 0) {
                    delay(waitTime)
                }
            }

            clock.advanceTo(simTargetTime)

            val wallNow = formattedWallTime()
            println("$wallNow [SimTime=${clock.now}ms] → ${change.name} = ${change.value}")

            change.apply()
        }
    }

    private fun formattedWallTime(): String {
        val now = System.currentTimeMillis()
        val hours = (now / (1000 * 60 * 60)) % 24
        val minutes = (now / (1000 * 60)) % 60
        val seconds = (now / 1000) % 60
        val millis = now % 1000

        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis)
    }
}