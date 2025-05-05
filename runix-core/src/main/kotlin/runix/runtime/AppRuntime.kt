package runix.runtime

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import runix.runtime.internal.RuntimeScope
import kotlin.reflect.KClass

/**
 * Central runtime manager for the entire app.
 * Entry point for launching apps programmatically.
 */
internal object AppRuntime {
    private var isRunning = false
    private var runningApp: App? = null

    /**
     * Starts an application and all its modules.
     * This is the main entry point for programmatic app launch.
     */
    suspend fun start(app: App) {
        check(!isRunning) { "AppRuntime is already running." }

        // Create and install the RuntimeScope
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        RuntimeScope.install(appScope)

        // Initialize runtime scheduler
        RuntimeScheduler.start()

        // Activate the app
        app.activate()
        
        // Store reference for shutdown
        runningApp = app
        isRunning = true
    }

    /**
     * Stops an application and all its modules.
     */
    suspend fun stop() {
        if (!isRunning) return

        try {
            // Gracefully deactivate the app
            runningApp?.deactivate()
        } finally {
            // Ensure scheduler is stopped even if deactivation has errors
            RuntimeScheduler.stop()
            shutDownRuntimeScope()

            runningApp = null
            isRunning = false
        }
    }

    private fun shutDownRuntimeScope() {
        try {
            // Explicitly cancel any remaining coroutines in the scope
            RuntimeScope.scope.cancel("AppRuntime shutdown")
        } catch (e: Exception) {
            // Log but don't rethrow to ensure cleanup continues
            System.err.println("Error canceling RuntimeScope: ${e.message}")
        } finally {
            // Clear the RuntimeScope reference
            RuntimeScope.clear()
        }
    }
}