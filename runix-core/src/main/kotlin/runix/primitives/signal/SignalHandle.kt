package runix.primitives.signal

import kotlinx.coroutines.withContext
import runix.tracing.TraceCollector
import runix.tracing.TraceEventContextElement
import runix.tracing.events.SignalEmitted
import runix.utils.Logger
import kotlin.coroutines.coroutineContext

/**
 * A strongly typed signal used for inter-module communication.
 *
 * Signals are *write-only* constructs designed for event-style emission and reaction.
 * Unlike state or flows, they do not hold history or allow observation outside the runtime.
 * They represent a message channel that can be:
 * - Emitted by any part of the system using `emit(value)`
 * - Reacted to by [ReactionHandle]s
 *
 * @param T the type of payload carried by this signal; often `Unit` for pure events
 * @property name a stable identifier used for tracing and interop
 */
class SignalHandle<T> internal constructor(
    val name: String
) {
    val logger = Logger.getLogger("$this")
    /**
     * Emits a new event of type [T].
     * Reactions or runtime handlers will be invoked accordingly.
     */
    suspend fun emit(value: T) {
        logger.debug { "Emitting signal with value $value" }
        val previousEvent = coroutineContext[TraceEventContextElement]?.previousEvent
        val signalEmittedEvent = SignalEmitted(
            parent = previousEvent,
            signalName = "$this"
        )
        TraceCollector.emit(signalEmittedEvent)

        withContext(TraceEventContextElement(signalEmittedEvent)) {
            SignalBus.emit(this@SignalHandle, value)
        }
    }

    override fun toString(): String = "Signal($name)"
}
