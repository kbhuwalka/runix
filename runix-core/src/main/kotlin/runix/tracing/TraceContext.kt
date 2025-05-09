package runix.tracing

import kotlinx.coroutines.ThreadContextElement
import java.util.UUID
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/**
 * Represents the causal relationship between traceable operations in the system.
 *
 * Each context carries a `traceId`, which uniquely identifies the current span of activity,
 * and an optional `parentId` that links it to the operation that caused it.
 *
 * Used to track causality through signal propagation, reactions, and actions.
 */
data class TraceContext(
    val traceId: UUID = UUID.randomUUID(),
    val parentId: UUID? = null
) {
    /**
     * Derives a child trace context from this one, assigning a new traceId and setting this as the parent.
     */
    fun derive(): TraceContext = TraceContext(parentId = this.traceId)

    companion object {
        /**
         * Creates a root-level trace context (no parent).
         */
        fun root(): TraceContext = TraceContext()
    }
}

suspend fun TraceContext.Companion.currentOrRoot(): TraceContext {
    return coroutineContext[TraceContextElement]?.context?.derive() ?: TraceContext.root()
}

/**
 * Coroutine context element used to propagate [TraceContext] implicitly across suspending boundaries.
 *
 * This ensures that trace data is automatically available during event emission, signal propagation,
 * or any traced execution path without needing to pass it explicitly.
 */
class TraceContextElement(
    val context: TraceContext
) : ThreadContextElement<TraceContext>,
    AbstractCoroutineContextElement(Key) {

    companion object Key : CoroutineContext.Key<TraceContextElement>

    override fun updateThreadContext(context: CoroutineContext): TraceContext = this.context
    override fun restoreThreadContext(context: CoroutineContext, oldState: TraceContext) {}
}