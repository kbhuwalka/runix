import runix.primitives.reaction.ReactionHandle
import runix.primitives.signal.SignalHandle

/**
 * Declare a reaction that runs the given [handler] when [signal] emits an event.
 *
 * Reactions must be installed into a module using `+reaction(...)` inside `defineBehavior`.
 */
fun <T> reaction(
    name: String,
    signal: SignalHandle<T>,
    handler: suspend (T) -> Unit
): ReactionHandle<T> {
    return ReactionHandle(name, signal, handler)
}