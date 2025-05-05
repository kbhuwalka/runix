package runix.runtime

/**
 * Interface for components that can be activated and deactivated
 * as part of the Runix application lifecycle.
 *
 * This is used for:
 * - App: The top-level application container
 * - AppModule: Individual behavior modules
 * - Monitors/Reactions: Individual primitives
 */
internal interface Activatable {
    /**
     * Activates this component. For:
     * - App: Activates all modules and runs didActivate hooks
     * - AppModule: Activates all primitives and runs didActivate hook
     * - Primitives: Starts monitoring or subscribes to signals
     */
    suspend fun activate()

    /**
     * Deactivates this component in preparation for shutdown.
     * Components should clean up resources and stop ongoing work.
     */
    suspend fun deactivate()
}