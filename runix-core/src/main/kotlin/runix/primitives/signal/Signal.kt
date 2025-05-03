package runix.primitives.signal

/**
 * A strongly typed signal used for inter-module communication.
 *
 * Emits events of type [T] and can be reacted to via a Reaction.
 */
class Signal<T> internal constructor(
    val name: String
) {
    /**
     * Emits a new signal event to any registered reactions.
     */
    fun emit(value: T) {
        // TODO: Dispatch to SignalBus
    }

    override fun toString(): String = "Signal<T>($name)"
}