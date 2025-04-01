package runix.tracing

import java.time.Duration
import java.time.Instant
import kotlin.time.toKotlinDuration

class ExecutionTimer(val startTime: Instant) {

    fun elapsed(): kotlin.time.Duration = Duration.between(startTime, Instant.now()).toKotlinDuration()

    companion object {
        fun start(): ExecutionTimer = ExecutionTimer(Instant.now())
    }
}

