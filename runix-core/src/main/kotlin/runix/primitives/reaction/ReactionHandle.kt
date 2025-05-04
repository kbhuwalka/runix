package runix.primitives.reaction

import runix.primitives.module.AppModule
import runix.primitives.signal.SignalHandle
import runix.runtime.ModuleScope
import runix.runtime.internal.Registerable
import runix.runtime.internal.RegistrationGuard
import runix.runtime.internal.SignalBus
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A declared reaction that listens to a [Signal] and runs a handler in response.
 *
 * Reactions are isRegistered inside [ModuleScope] using `+reaction(...)`.
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
): Registerable {
    private val guard = RegistrationGuard()

    override fun register(module: AppModule) {
        guard.register(module)
        SignalBus.register(signal, handler)
    }

    override fun toString(): String = "Reaction(${name})"
}