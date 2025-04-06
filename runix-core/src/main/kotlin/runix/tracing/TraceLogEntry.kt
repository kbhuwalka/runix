package runix.tracing

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class TraceLogEntry(
    val id: Long,
    val parentId: Long?,
    val type: String,
    val name: String,
    @Contextual val timestamp: Instant,
    val durationMs: Long,
    val status: ExecutionStatus,
    @Contextual val startTime: Instant? = null,
    @Contextual val endTime: Instant? = null,
    val exception: String? = null,
    val actor: String? = null,
    val tags: List<String> = emptyList(),
    val causeTrace: CauseTrace? = null,
    val tracePath: List<String> = emptyList(),
    val context: Map<String, String> = emptyMap()
) {
    @Serializable
    data class CauseTrace(
        val traceId: Long,
        val reason: String
    )
}
