package runix.primitives.signal

import runix.runtime.internal.SignalBus

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
    /**
     * Emits a new event of type [T].
     * Reactions or runtime handlers will be invoked accordingly.
     */
    fun emit(value: T) {
        SignalBus.emit(this, value)
    }

    override fun toString(): String = "Signal($name)"
}
