package runix.primitives.reaction

import runix.primitives.module.AppModule
import runix.primitives.signal.SignalHandle
import runix.runtime.internal.Registerable
import runix.runtime.internal.RegistrationGuard
import runix.runtime.internal.SignalBus

class ReactionHandle<T> internal constructor(
    val name: String,
    private val signal: SignalHandle<T>,
    private val handler: suspend (T) -> Unit
) : Registerable {
    private val guard = RegistrationGuard()

    override fun register(module: AppModule) {
        guard.register(module)
        SignalBus.register(signal, handler)
    }

    override fun toString(): String = "Reaction($name)"
}
