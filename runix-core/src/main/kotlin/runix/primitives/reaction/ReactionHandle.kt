package runix.primitives.reaction

import reaction
import runix.runtime.Activatable
import runix.primitives.module.AppModule
import runix.primitives.signal.SignalHandle
import runix.runtime.internal.Registerable
import runix.runtime.internal.RegistrationGuard
import runix.runtime.internal.SignalBus
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Represents a declared reaction that responds to a specific signal.
 * Created using the [reaction] function.
 */
class ReactionHandle<T> internal constructor(
    val name: String,
    private val signal: SignalHandle<T>,
    private val handler: suspend (T) -> Unit
) : Registerable, Activatable {
    private val guard = RegistrationGuard()
    private var activated = AtomicBoolean(false)

    /**
     * Registers this reaction with a module.
     * This establishes ownership but doesn't activate the reaction yet.
     */
    override fun register(module: AppModule) {
        guard.register(module)
    }
    
    /**
     * Activates this reaction by registering its handler with the SignalBus.
     */
    override fun activate() {
        check(guard.isRegistered()) { "Reaction '$name' must be registered before activation." }
        if (activated.get()) return
        
        SignalBus.register(signal, handler)
        activated.set(true)
    }
    
    /**
     * Deactivates this reaction by unregistering its handler from the SignalBus.
     */
    override fun deactivate() {
        if (!activated.get()) return
        
        SignalBus.unregister(signal, handler)
        activated.set(false)
    }

    override fun toString(): String = "Reaction($name)"
}