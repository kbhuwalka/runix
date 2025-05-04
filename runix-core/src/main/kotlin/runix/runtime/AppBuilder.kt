package runix.runtime

import runix.primitives.module.AppModule

/**
 * Internal builder for declarative construction of a [App].
 *
 * Supports registration of [Installable]s (including [Module]s),
 * and lifecycle hooks for startup and shutdown logic.
 */
class AppBuilder {
    private val modules = mutableListOf<AppModule>()
    private var startHook: (suspend () -> Unit)? = null
    private var stopHook: (suspend () -> Unit)? = null

    /**
     * Registers one or more [AppModule]s into the application.
     *
     * Modules define robot behavior — including conditions, actions, and reactions.
     * This is the primary way to compose an app in Runix.
     */
    fun install(vararg entries: AppModule) {
        modules += entries
    }

    /**
     * Adds an onStart lifecycle hook to be invoked after installables,
     * but before modules are started.
     */
    fun onStart(block: suspend () -> Unit) {
        check(startHook == null) { "onStart already defined" }
        startHook = block
    }

    /**
     * Adds an onStop lifecycle hook to be invoked after modules are stopped.
     */
    fun onStop(block: suspend () -> Unit) {
        check(stopHook == null) { "onStop already defined" }
        stopHook = block
    }

    internal fun build(): App = App(
        modules = modules.toList(),
        startHook = startHook,
        stopHook = stopHook
    )
}
