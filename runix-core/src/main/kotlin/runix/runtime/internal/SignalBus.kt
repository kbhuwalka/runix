package runix.runtime.internal

import runix.primitives.signal.SignalHandle

/**
 * Internal message router for `SignalHandle` emissions.
 *
 * The `SignalBus` dispatches emitted signals to registered reactions across modules.
 * All signal emissions go through this central bus, ensuring:
 * - Emission tracing and runtime introspection
 * - Debug visibility and diagnostics
 * - Decoupled dispatch and handler registration
 *
 * Currently a stub. Future responsibilities will include:
 * - Reaction registration
 * - Scheduling execution via coroutine scope
 * - Delayed signal dispatch, backpressure, or coalescing (if needed)
 */
internal object SignalBus {

    /**
     * Emits a value through the given signal.
     *
     * This is the core entry point for all `signal.emit(...)` calls.
     * Runtime reactions should be notified here (in future work).
     *
     * @param signal The declared [SignalHandle] being emitted
     * @param value The data payload associated with the signal emission
     */
    fun <T> emit(signal: SignalHandle<T>, value: T) {
        // TODO: Dispatch to all reactions registered to this signal
        // This is a stub; the routing mechanism will be implemented in Milestone 4.
    }

    /**
     * Registers a reaction to a signal.
     *
     * This method will be used by the Reaction primitive at registration time.
     * Reactions will be stored in a per-signal list and executed when the signal is emitted.
     */
    fun <T> register(signal: SignalHandle<T>, reaction: suspend (T) -> Unit) {
        // TODO: Add reaction to internal signal → listeners map
        // This may be a ConcurrentHashMap<SignalHandle<*>, MutableList<...>>
    }

    /**
     * Unregisters a specific handler from a signal.
     * Used during deactivation to clean up reaction handlers.
     */
    fun <T> unregister(signal: SignalHandle<T>, handler: suspend (T) -> Unit) {
        // Implementation details depend on how handlers are stored
        // This would remove the specific handler from the signal's subscription list
    }


    /**
     * Unregisters all reactions for cleanup (e.g. module shutdown).
     */
    fun reset() {
        // TODO: Clear registered listeners (if needed for test isolation or shutdown)
    }
}
