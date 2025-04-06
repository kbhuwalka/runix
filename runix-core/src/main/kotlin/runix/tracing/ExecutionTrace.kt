package runix.tracing

import runix.core.RunixScheduler
import runix.core.logger
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

object TraceIdGenerator {
    private val counter = AtomicLong(0)
    fun nextId(): Long = counter.incrementAndGet()
}

data class ExecutionTrace(
    val id: Long = TraceIdGenerator.nextId(),
    val parentId: Long? = null,
    val path: List<String> = emptyList(),
    val scheduler: RunixScheduler,
    val startTime: Instant = Instant.now(),
    var endTime: Instant? = null,
    var exception: Throwable? = null,
    val actor: String? = null,
    val tags: List<String> = emptyList(),
    val causeTraceId: Long? = null
) {
    private val lastSignalTraces = mutableMapOf<String, ExecutionTrace>()

    override fun toString(): String = path.joinToString(" → ")

    fun logSuccess(msg: String) {
        logger.info { "✅ [$this] $msg" }
    }

    fun logFailure(reason: String) {
        logger.warn { "❌ [$this] Failure: $reason" }
    }

    fun logSkipped(reason: String) {
        logger.info { "⏭️ [$this] Skipped: $reason" }
    }

    fun logTimeout(reason: String) {
        logger.warn { "⏰ [$this] Timeout: $reason" }
    }

    fun logCancellation(reason: String) {
        logger.debug { "⏰ [$this] Cancelled: $reason" }
    }

    fun lastSignalTrace(signalName: String): ExecutionTrace? {
        return lastSignalTraces[signalName]
    }
}
