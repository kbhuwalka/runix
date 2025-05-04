package runix.runtime

import kotlinx.coroutines.cancel
import runix.runtime.internal.RuntimeScope
import runix.primitives.module.AppModule

/**
 * The top-level container and execution context for a Runix application.
 *
 * A App defines the cognitive runtime of a robot: it wires together
 * behavior modules, starts and stops them explicitly, and provides optional
 * lifecycle hooks for external integration or setup logic.
 *
 * Developers create an App declaratively using the DSL:
 *
 * ```
 * val app = App {
 *   install(PowerModule)
 *   install(RecoveryModule)
 *
 *   onStart { println("App ready") }
 *   onStop { println("Clean shutdown") }
 * }
 *
 * runBlocking { app.start() }
 * ```
 *
 * @constructor Use [App] DSL entrypoint to construct an instance.
 * @property modules The set of modules or installable components
 * @property startHook Suspendable lambdas invoked after modules, before modules
 * @property stopHook Suspendable lambdas invoked on shutdown
 */
class App internal constructor(
    private val modules: List<AppModule>,
    private val startHook: (suspend () -> Unit)?,
    private val stopHook: (suspend () -> Unit)?
) {
    private val scope = RuntimeScope.scope

    /**
     * Starts the application: installs components, runs onStart hooks,
     * and activates all registered modules.
     */
    suspend fun start() {
        modules.forEach { it.install(this) }
        startHook?.invoke()
        modules.forEach { it.onStart() }
    }

    /**
     * Shuts down the application: deactivates modules and runs cleanup hooks.
     */
    suspend fun stop() {
        modules.reversed().forEach { it.onStop() }
        stopHook?.invoke()
        scope.cancel("RunixApp shutdown")
    }

    companion object {
        /**
         * DSL entrypoint for building a Runix application.
         *
         * Use `install(...)` to register modules and components,
         * and `onStart {}` / `onStop {}` to customize lifecycle behavior.
         */
        operator fun invoke(init: AppBuilder.() -> Unit): App {
            return AppBuilder().apply(init).build()
        }
    }
}
