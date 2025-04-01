package runix.tracing

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

data class TraceNode(
    val id: Long,
    val name: String,
    val type: String,
    val parentId: Long? = null,
    val startedAt: Instant = Instant.now(),
    var endedAt: Instant? = null,
    var status: String = "Running",
    val children: MutableList<TraceNode> = mutableListOf()
)