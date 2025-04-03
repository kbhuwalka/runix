package runix.tracing

import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class LiveTraceManager {

    private val roots = ConcurrentHashMap<Long, TraceNode>()
    private val allNodes = ConcurrentHashMap<Long, TraceNode>()

    fun start(trace: TraceNode) {
        allNodes[trace.id] = trace
        if (trace.parentId == null) {
            roots[trace.id] = trace
        } else {
            allNodes[trace.parentId]?.children?.add(trace)
        }
    }

    fun complete(id: Long, status: String) {
        val node = allNodes[id]
        node?.apply {
            this.status = status
            this.endedAt = Instant.now()
        }
    }

    fun expireOld(maxAge: Duration = Duration.ofMinutes(5)) {
        val now = Instant.now()
        roots.entries.removeIf { (_, node) ->
            node.endedAt?.let { Duration.between(it, now) > maxAge } == true
        }
    }

    fun getActiveTrees(): List<TraceNode> = roots.values.toList()
}
