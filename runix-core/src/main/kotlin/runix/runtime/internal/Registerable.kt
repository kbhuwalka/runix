package runix.runtime.internal

import runix.primitives.module.AppModule

/**
 * Internal marker for handles that need registration with a module
 * during `defineBehavior {}`.
 */
internal interface Registerable {
    /**
     * Register this handle with the given module.
     * This should only be called once during app setup.
     */
    fun register(module: AppModule)
}
