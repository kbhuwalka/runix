package runix.primitives.reaction

import runix.primitives.signal.SignalHandle
import runix.runtime.ModuleScope
import runix.runtime.internal.SignalBus

/**
 * A declared reaction that listens to a [Signal] and runs a handler in response.
 *
 * Reactions are registered inside [ModuleScope] using `+reaction(...)`.
 *
 * Example:
 * ```
 * val r = reaction(batteryLowSignal) {
 *   stopAllActions()
 * }
 * ```
 */
class ReactionHandle<T> internal constructor(
    val name: String,
    private val signal: SignalHandle<T>,
    private val handler: suspend (T) -> Unit
) {
    private var registered = false

    internal fun register(module: String) {
        check(!registered) {
            "Reaction for signal '${signal.name}' is already registered to a module."
        }
        registered = true

        // Hook into the signal bus for event dispatch
        SignalBus.register(signal, handler)
    }

    override fun toString(): String = "Reaction(${name})"
}