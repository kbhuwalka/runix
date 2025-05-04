package runix.primitives.module

import runix.runtime.Activatable
import kotlinx.coroutines.launch
import runix.primitives.action.ActionHandle
import runix.primitives.monitor.MonitorHandle
import runix.primitives.reaction.ReactionHandle
import runix.runtime.App
import runix.runtime.DefaultModuleScope
import runix.runtime.ModuleScope
import runix.runtime.internal.BehaviorRegistry
import runix.runtime.internal.RuntimeScope

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
 *
 * Example:
 * ```
 * object PowerModule : AppModule("PowerModule") {
 *   // Declare primitives
 *   private val batteryLow = monitor("batteryLow") {
 *     batteryLevel < 20.0
 *   } emit batteryLowSignal
 * 
 *   // Register behavior directly in class body
 *   defineBehavior {
 *     +batteryLow
 *     +powerOffReaction
 *   }
 * 
 *   // Set lifecycle hooks directly
 *   didStart { 
 *     logger.info("Power module active")
 *   }
 *   
 *   willStop {
 *     logger.info("Power module shutting down")
 *   }
 * }
 * ```
 */
abstract class AppModule(
    val name: String
) : Activatable {
    // Primitives collections
    private val monitors = mutableListOf<MonitorHandle>()
    private val reactions = mutableListOf<ReactionHandle<*>>()
    private val actions = mutableListOf<ActionHandle<*>>()
    
    // Parent app reference, set during installation
    private var app: App? = null
    
    // Lifecycle hooks
    private var didStartBlock: (suspend () -> Unit)? = null
    private var willStopBlock: (suspend () -> Unit)? = null
    
    /**
     * Defines behavior for this module by registering primitives.
     * Can be called directly in the module's class body.
     */
    fun defineBehavior(block: ModuleScope.() -> Unit) {
        BehaviorRegistry.claim(this)
        
        val scope = DefaultModuleScope()
        scope.block()
        
        // Store primitives for lifecycle management
        monitors.addAll(scope.getMonitors())
        reactions.addAll(scope.getReactions())
        actions.addAll(scope.getActions())
        
        // Register all primitives
        monitors.forEach { it.register(this) }
        reactions.forEach { it.register(this) }
        actions.forEach { it.register(this) }
    }
    
    /**
     * Register a callback to be invoked after all primitives are activated.
     * Can be called directly in the module's class body.
     */
    fun didStart(block: suspend () -> Unit) {
        didStartBlock = block
    }
    
    /**
     * Register a callback to be invoked before primitives are deactivated.
     * Can be called directly in the module's class body.
     */
    fun willStop(block: suspend () -> Unit) {
        willStopBlock = block
    }
    
    /**
     * Sets the parent app for this module. Called internally by App during installation.
     */
    internal fun setApp(app: App) {
        this.app = app
    }
    
    /**
     * Activates all primitives in this module.
     * Called internally by the runtime - do not call directly.
     */
    override fun activate() {
        // Start all primitives
        monitors.forEach { it.activate() }
        reactions.forEach { it.activate() }
        
        // Execute developer hook
        RuntimeScope.scope.launch {
            didStartBlock?.invoke()
        }
    }
    
    /**
     * Deactivates all primitives in this module.
     * Called internally by the runtime - do not call directly.
     */
    override fun deactivate() {
        // Execute developer hook first
        RuntimeScope.scope.launch {
            willStopBlock?.invoke()
        }
        
        // Then stop all primitives in reverse order
        reactions.asReversed().forEach { it.deactivate() }
        monitors.asReversed().forEach { it.deactivate() }
    }
}