package runix.tracing

import java.util.UUID

/**
 * TraceContext represents the causal linkage between related trace events.
 * It provides identifiers for trace correlation, nesting, and session scoping.
 *
 * @param traceId: unique identifier for this trace flow (typically spans one action or signal path)
 * @param parentId: references the traceId that caused this one (for nesting/causality)
 */
data class TraceContext(
    val traceId: UUID = UUID.randomUUID(),
    val parentId: UUID? = null,
) {
    /**
     * Derives a new context for a child operation.
     * Assigns a new traceId and sets this context's traceId as parentId.
     */
    fun derive(): TraceContext {
        return TraceContext(parentId = this.traceId)
    }

    /**
     * Creates a new context for a standalone action or signal.
     */
    companion object {
        fun root(): TraceContext = TraceContext()
    }
}