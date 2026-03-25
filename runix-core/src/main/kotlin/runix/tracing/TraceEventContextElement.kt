package runix.tracing

import kotlinx.coroutines.ThreadContextElement
import runix.tracing.events.TraceEvent
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Coroutine context element that propagates [TraceEvent] instances across suspending boundaries.
 *
 * This element maintains causal relationships between events in asynchronous execution flows by:
 * 1. Carrying a reference to the previous event in the execution chain
 * 2. Making this reference available to child coroutines
 * 3. Allowing new events to link to their causal parent
 *
 * Usage pattern:
 * - Retrieve the previous event: `coroutineContext[TraceEventContextElement]?.previousEvent`
 * - Create a new event with this as parent
 * - Use withContext to propagate the new event: `withContext(TraceEventContextElement(newEvent)) { ... }`
 *
 * This eliminates the need for a separate trace context object while maintaining proper
 * event causality through coroutine boundaries.
 */
internal class TraceEventContextElement(
    val previousEvent: TraceEvent
) : ThreadContextElement<TraceEvent>,
    AbstractCoroutineContextElement(Key) {

    companion object Key : CoroutineContext.Key<TraceEventContextElement>

    override fun updateThreadContext(context: CoroutineContext): TraceEvent = this.previousEvent
    override fun restoreThreadContext(context: CoroutineContext, oldState: TraceEvent) {}
}