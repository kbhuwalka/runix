package runix.runtime.internal

import runix.primitives.module.AppModule

/**
 * Internal registry to enforce that `defineBehavior` is only called once per module.
 */
internal object BehaviorRegistry {
    private val definedModules = mutableSetOf<AppModule>()

    fun claim(module: AppModule) {
        check(definedModules.add(module)) {
            "Behavior for module '${module.name}' was already defined. You may only call defineBehavior once."
        }
    }
}