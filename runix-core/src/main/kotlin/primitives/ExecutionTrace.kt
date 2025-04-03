package runix.primitives.tracing

import runix.core.logger
import runix.primitives.RunixScheduler
import java.util.concurrent.atomic.AtomicLong

object TraceIdGenerator {
    private val counter = AtomicLong(0)
    fun nextId(): Long = counter.incrementAndGet()
}

data class ExecutionTrace(
    val id: Long = TraceIdGenerator.nextId(),
    val parentId: Long? = null,
    val path: List<String> = emptyList(),
    val scheduler: RunixScheduler
) {
    override fun toString(): String = path.joinToString(" → ")

    fun logSuccess(msg: String) {
        logger.info { "✅ [$this] $msg" }
    }

    fun logFailure(reason: String, recoverable: Boolean) {
        val label = if (recoverable) "Recoverable" else "Fatal"
        logger.warn { "❌ [$this] $label failure: $reason" }
    }

    fun logSkipped(reason: String) {
        logger.info { "⏭️ [$this] Skipped: $reason" }
    }

    fun logTimeout(reason: String) {
        logger.warn { "⏰ [$this] Timeout: $reason" }
    }
}

fun ExecutionTrace.child(name: String): ExecutionTrace = ExecutionTrace(
    parentId = this.id,
    path = this.path + name,
    scheduler = this.scheduler
)