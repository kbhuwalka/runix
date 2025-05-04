package runix.runtime

import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import runix.runtime.internal.RuntimeScope
import runix.primitives.module.AppModule

/**
 * The top-level container and execution context for a Runix application.
 *
 * A App defines the cognitive runtime of a robot: it wires together
 * behavior modules, starts and stops them explicitly, and provides optional
 * lifecycle hooks for external integration or setup logic.
 *
 * Developers create an App declaratively:
 *
 * ```
 * object RobotApp : App {
 *   // Install modules directly in class body
 *   install(PowerModule)
 *   install(RecoveryModule)
 *
 *   // Set lifecycle hooks directly
 *   didStart {
 *     println("App ready")
 *   }
 *
 *   willStop {
 *     println("Clean shutdown")
 *   }
 * }
 * ```
 */
abstract class App : Activatable {
    private val modules = mutableListOf<AppModule>()
    private var didStartBlock: (suspend () -> Unit)? = null
    private var willStopBlock: (suspend () -> Unit)? = null
    
    /**
     * Installs a module into this app.
     * Can be called directly in the app's class body.
     */
    fun install(module: AppModule) {
        modules.add(module)
        module.setApp(this)
    }
    
    /**
     * Register a hook to be called after all modules are activated.
     * Can be called directly in the app's class body.
     */
    fun didStart(block: suspend () -> Unit) {
        didStartBlock = block
    }
    
    /**
     * Register a hook to be called before any modules are deactivated.
     * Can be called directly in the app's class body.
     */
    fun willStop(block: suspend () -> Unit) {
        willStopBlock = block
    }
    
    /**
     * Start the application: install components, activate all modules,
     * and run the didStart hook.
     */
    override fun activate() {
        // Activate all modules
        modules.forEach { it.activate() }
        
        // Execute developer hook
        RuntimeScope.scope.launch {
            didStartBlock?.invoke()
        }
    }
    
    /**
     * Gracefully shutdown the application: run the willStop hook,
     * deactivate all modules in reverse order.
     */
    override fun deactivate() {
        // Execute developer hook first
        RuntimeScope.scope.launch {
            willStopBlock?.invoke()
        }
        
        // Then deactivate all modules in reverse order
        modules.asReversed().forEach { it.deactivate() }
        
        // Finally cancel the runtime scope
        RuntimeScope.scope.cancel("App shutdown")
    }
}