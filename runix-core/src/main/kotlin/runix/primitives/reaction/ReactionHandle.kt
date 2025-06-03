package runix.primitives.reaction

import kotlinx.coroutines.withContext
import runix.primitives.module.AppModule
import runix.primitives.signal.SignalBus
import runix.primitives.signal.SignalHandle
import runix.runtime.Activatable
import runix.runtime.internal.Registerable
import runix.runtime.internal.RegistrationGuard
import runix.tracing.TraceCollector
import runix.tracing.TraceEventContextElement
import runix.tracing.events.ReactionTriggered
import runix.utils.Logger
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.coroutineContext

/**
 * Represents a declared reaction that responds to a specific signal.
 * Created using the [reaction] function.
 */
class ReactionHandle<T> internal constructor(
    val name: String,
    private val signal: SignalHandle<T>,
    internal val handler: suspend (T) -> Unit
) : Registerable, Activatable {
    val logger = Logger.getLogger("$this")
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
    override suspend fun activate() {
        check(guard.isRegistered()) { "Reaction '$name' must be registered before activation." }
        if (activated.get()) return

        logger.info("Registering reaction to $signal")
        
        SignalBus.register(signal, this)
        activated.set(true)
    }
    
    /**
     * Deactivates this reaction by unregistering its handler from the SignalBus.
     */
    override suspend fun deactivate() {
        if (!activated.get()) return
        logger.info("Unregistering reaction from $signal")

        SignalBus.unregister(signal, this)
        activated.set(false)
    }

    internal suspend fun executeReaction(value: T) {
        logger.debug{ "Executing reaction to signal $signal" }
        val previousEvent = coroutineContext[TraceEventContextElement]?.previousEvent
        val reactionTriggeredEvent = ReactionTriggered(
            parent = previousEvent,
            reactionName = "$this"
        )
        TraceCollector.emit(reactionTriggeredEvent)

        withContext(TraceEventContextElement(reactionTriggeredEvent)) {
            handler.invoke(value)
        }
    }

    override fun toString(): String = "Reaction($name)"
}