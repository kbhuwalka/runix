import runix.primitives.reaction.ReactionHandle
import runix.primitives.signal.SignalHandle

/**
 * Declares a reaction that runs a handler when a specific signal is emitted.
 *
 * Reactions are the reactive glue of the system, acting as reflexes that respond
 * to signals from any part of the application. They enable loosely coupled communication
 * between modules and allow components to react to events without direct dependencies.
 *
 * Key characteristics:
 * - Triggered automatically when their associated signal is emitted
 * - Run asynchronously in a coroutine context
 * - Strongly typed to ensure type-safety with the signal payload
 * - Lightweight and designed for quick responses
 *
 * Example:
 * ```kotlin
 * // Simple reaction to a parameterless signal
 * val onSystemStartup = reaction(
 *   name = "logStartup",
 *   signal = systemStartedSignal
 * ) {
 *   logger.info("System started successfully")
 * }
 * ```
 *
 * To make a reaction active, register it using the unary plus operator in a module's
 * defineBehavior method.
 *
 * @param name A descriptive name for this reaction, used in logging and debugging
 * @param signal The signal this reaction responds to
 * @param handler A suspending function that executes when the signal is emitted.
 *                Receives the signal's payload as its parameter.
 * @return A [ReactionHandle] that can be registered with in an [AppModule] to activate the reaction.
 */

fun <T> reaction(
    name: String,
    signal: SignalHandle<T>,
    handler: suspend (T) -> Unit
): ReactionHandle<T> {
    return ReactionHandle(name, signal, handler)
}
