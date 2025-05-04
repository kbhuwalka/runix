package runix.runtime

import kotlin.reflect.KClass

/**
 * Central runtime manager for the entire Runix app.
 * Entry point for launching apps programmatically.
 */
object AppRuntime {
    private var isRunning = false
    private var runningApp: App? = null

    /**
     * Starts an application and all its modules.
     * This is the main entry point for programmatic app launch.
     */
    fun start(app: App) {
        check(!isRunning) { "AppRuntime is already running." }

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
    fun stop(app: App) {
        if (!isRunning) return

        try {
            // Gracefully deactivate the app
            app.deactivate()
        } finally {
            // Ensure scheduler is stopped even if deactivation has errors
            RuntimeScheduler.stop()
            runningApp = null
            isRunning = false
        }
    }
    
    /**
     * Stops the currently running app if any.
     * Used by shutdown hooks when the app reference isn't directly available.
     */
    fun stopRunningApp() {
        runningApp?.let { stop(it) }
    }

    /**
     * Discovers and starts the first available App in the classpath.
     * Used for automatic startup without developers needing to write main().
     */
    fun discoverAndStart() {
        val appClass = App::class.sealedSubclasses.firstOrNull()
            ?: App::class.nestedClasses.filterIsInstance<KClass<App>>().firstOrNull()
            ?: throw IllegalStateException("No App implementation found")

        val app = appClass.objectInstance
            ?: throw IllegalStateException("App must be an object")

        start(app)
    }
}