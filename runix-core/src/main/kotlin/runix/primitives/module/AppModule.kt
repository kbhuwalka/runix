package runix.primitives.module

import runix.annotations.Internal
import runix.runtime.App
import runix.runtime.Installable

/**
 * A behavioral unit in a Runix application that declares and owns:
 * - Monitors (which observe conditions)
 * - Reactions (which respond to signals)
 * - Actions (which perform time-aware operations)
 *
 * AppModules should be:
 * - Self-contained
 * - Declarative
 * - Explicitly composable
 * - Testable in isolation
 */
@OptIn(Internal::class)
interface AppModule : Installable {
    /**
     * A stable name used for traceability and diagnostics.
     */
    val name: String

    /**
     * Called when the app is started and the module should begin execution.
     * Use this to register monitors, start behaviors, etc.
     */
    fun onStart()

    /**
     * Called when the app is shutting down. Use to cancel flows or clean up.
     */
    fun onStop() {}

    /**
     * DO NOT call manually. This is invoked internally by the App to allow
     * the framework to register lifecycle structure. Behavior must be declared in [onStart].
     */
    override fun install(app: App) {
        // Default install does nothing; the app will call onStart/onStop explicitly
    }
}
