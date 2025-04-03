package runix.tracing

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.Instant

enum class ExecutionStatus {
    Success,
    Skipped,
    Failure,
    Timeout,
    Triggered
}

@Serializable
data class TraceLogEntry(
    val id: Long,
    val parentId: Long?,
    val type: String,
    val name: String,
    @Contextual val timestamp: Instant,
    val durationMs: Long,
    val status: ExecutionStatus,
    val cause: Cause? = null,
    val tracePath: List<String> = emptyList(),
    val context: Map<String, String> = emptyMap()
) {
    @Serializable
    data class Cause(
        val type: String,
        val name: String
    )
}
