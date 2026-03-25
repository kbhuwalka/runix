package runix.runtime

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import runix.runtime.internal.RuntimeScope
import runix.utils.Logger

/**
 * Central runtime manager for the runtime environment.
 * Handles low-level runtime initialization and cleanup.
 */
internal object AppRuntime {
    private var isRunning = false
    val logger = Logger.getLogger<AppRuntime>()

    /**
     * Initializes the runtime environment.
     * This sets up the coroutine scope and starts the scheduler.
     */
    fun initialize() {
        check(!isRunning) { "AppRuntime is already running." }

        // Create and install the RuntimeScope
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        RuntimeScope.install(appScope)
        
        isRunning = true
    }

    /**
     * Shuts down the runtime environment.
     * This stops the scheduler and cancels any ongoing coroutines.
     */
    fun shutdown() {
        if (!isRunning) return
        shutDownRuntimeScope()
        isRunning = false
    }

    private fun shutDownRuntimeScope() {
        try {
            // Explicitly cancel any remaining coroutines in the scope
            RuntimeScope.scope.cancel("AppRuntime shutdown")
        } catch (e: Exception) {
            // Log but don't rethrow to ensure cleanup continues
            logger.error { "Runtime encountered error: ${e.message}" }
        } finally {
            // Clear the RuntimeScope reference
            RuntimeScope.clear()
        }
    }
}