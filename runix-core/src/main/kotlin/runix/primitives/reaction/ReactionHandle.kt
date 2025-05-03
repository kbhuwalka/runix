package runix.primitives.reaction

/**
 * Represents a declared reaction that listens to a signal and responds with logic.
 *
 * Must be registered in a ModuleScope to be active.
 */
class ReactionHandle internal constructor(
    private val signalName: String,
    private val handler: suspend () -> Unit
) {
    private var isRegistered = false

    internal fun register(module: String) {
        check(!isRegistered) {
            "Reaction for signal '$signalName' already registered in module '$module'."
        }
        isRegistered = true

        // TODO: Register with SignalBus
    }
}