package runix.runtime

import runix.primitives.module.AppModule
import runix.runtime.internal.BehaviorRegistry

/**
 * Declarative entrypoint for defining a module's behavior.
 *
 * This is the only valid way to register monitors, actions, or reactions.
 * It must be called exactly once inside an [AppModule.onStart] method.
 *
 * Example:
 * ```
 * override fun onStart() = defineBehavior {
 *   +batteryLow
 *   +dockAction
 * }
 * ```
 */
fun AppModule.defineBehavior(block: ModuleScope.() -> Unit) {
    BehaviorRegistry.claim(this)

    val scope = DefaultModuleScope(module = this)
    scope.block()
    scope.activate()
}
