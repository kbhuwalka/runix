package runix.primitives.action

/**
 * Represents a declared, named action that performs long-running logic.
 *
 * Must be registered in a ModuleScope before it can be run.
 */
class ActionHandle<T> internal constructor(
    private val name: String,
    private val logic: suspend ActionContext.() -> T
) {
    private var isRegistered = false

    internal fun register(module: String) {
        check(!isRegistered) {
            "Action '$name' already registered in module '$module'."
        }
        isRegistered = true

        // TODO: Add to runtime scheduler
    }

    fun isRunning(): Boolean {
        check(isRegistered) {
            "Action '$name' has not been registered. Cannot check status."
        }
        // TODO: Connect to action lifecycle manager
        return false
    }

    fun cancel() {
        check(isRegistered) {
            "Action '$name' has not been registered. Cannot cancel."
        }
        // TODO: Forward to runtime cancel logic
    }

    suspend fun run(): T {
        check(isRegistered) {
            "Action '$name' has not been registered. Call `+action` inside defineBehavior {}."
        }
        // TODO: Execute action logic
        return ActionContext().logic()
    }
}