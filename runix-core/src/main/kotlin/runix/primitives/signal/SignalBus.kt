package runix.primitives.signal

import kotlinx.coroutines.launch
import runix.primitives.reaction.ReactionHandle
import runix.runtime.internal.RuntimeScope
import java.util.concurrent.ConcurrentHashMap

/**
 * Internal message router for `SignalHandle` emissions.
 *
 * The `SignalBus` dispatches emitted signals to registered reactions across modules.
 * All signal emissions go through this central bus, ensuring:
 * - Emission tracing and runtime introspection
 * - Debug visibility and diagnostics
 * - Decoupled dispatch and handler registration
 */
internal object SignalBus {
    // Map of signals to sets of ReactionHandles
    private val registrations = ConcurrentHashMap<SignalHandle<*>, MutableSet<ReactionHandle<*>>>()

    /**
     * Emits a value through the given signal.
     *
     * This is the core entry point for all `signal.emit(...)` calls.
     * Dispatches to all registered reactions for this signal.
     *
     * @param signal The declared [SignalHandle] being emitted
     * @param value The data payload associated with the signal emission
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun <T> emit(signal: SignalHandle<T>, value: T) {
        // Get all reaction handles registered for this signal
        val reactions = registrations[signal] ?: return

        // Launch each reaction handler in the runtime scope with error handling
        reactions.forEach { reaction ->
            RuntimeScope.scope.launch {
                try {
                    val typedReaction = reaction as ReactionHandle<T>
                    typedReaction.executeReaction(value)
                } catch (e: Exception) {
                    // Log error but don't let it crash the system
                    System.err.println("Error in reaction '${reaction.name}' to signal '${signal.name}': ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Registers a reaction handler to a signal.
     *
     * This method is used by the ReactionHandle at activation time.
     * The full ReactionHandle is stored to allow for better tracing and error reporting.
     *
     * @param signal The signal to register for
     * @param handler The original handler to be called (not used for storage)
     */
    fun <T> register(signal: SignalHandle<T>, reaction: ReactionHandle<T>) {
        // Create the set if it doesn't exist, then add the reaction handle
        registrations.getOrPut(signal) { mutableSetOf() }
            .add(reaction)
    }

    /**
     * Unregisters a reaction handler from a signal.
     * Used during deactivation to clean up reaction handlers.
     *
     * @param signal The signal to unregister from
     * @param reaction The reaction to remove
     */
    fun <T> unregister(signal: SignalHandle<T>, reaction: ReactionHandle<T>) {
        registrations[signal]?.remove(reaction)

        // If there are no more reactions for this signal, remove the entry
        if (registrations[signal]?.isEmpty() == true) {
            registrations.remove(signal)
        }
    }

    /**
     * Unregisters all reactions for cleanup (e.g. module shutdown).
     * Primarily used for testing and shutdown scenarios.
     */
    fun reset() {
        registrations.clear()
    }
}