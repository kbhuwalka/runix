package runix.runtime

import kotlinx.coroutines.runBlocking
import runix.primitives.module.AppModule
import runix.tracing.TraceCollector
import runix.tracing.reporters.ConsoleTraceReporter
import java.util.concurrent.CountDownLatch

/**
 * The top-level container and execution context for a Runix application.
 *
 * An App defines the cognitive runtime of a robot: it wires together
 * behavior modules, starts and stops them explicitly, and provides optional
 * lifecycle hooks for external integration or setup logic.
 *
 * Developers create an App declaratively and start it directly:
 *
 * ```
 * object RobotApp : App() {
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
 *
 * fun main() {
 *   RobotApp.start() // Start the app and block until shutdown
 * }
 * ```
 */
abstract class App : Activatable {
    protected val modules = mutableListOf<AppModule>()
    private var didStartBlock: (suspend () -> Unit)? = null
    private var willStopBlock: (suspend () -> Unit)? = null
    private var isRunning = false

    // Latch to coordinate shutdown between threads
    internal val shutdownLatch = CountDownLatch(1)
    
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
     * Starts this application and all its modules.
     * This is the main entry point for running applications.
     * 
     * When called, this method:
     * 1. Initializes runtime environment
     * 2. Activates all modules in registration order
     * 3. Calls the didStart hooks
     * 4. Blocks the current thread until shutdown is requested
     * 5. Registers JVM shutdown hooks for clean termination on SIGINT/SIGTERM
     * 
     * Usage:
     * ```
     * fun main() {
     *    MyApp().start() // Blocks until app terminates
     * }
     * ```
     */
    fun start() {
        // Set up a JVM shutdown hook for graceful termination
        setupShutdownHook()
        
        // Start an app and block until terminated
        runBlocking {
            startAsync()
            
            // Block until explicitly stopped or JVM shutdown
            shutdownLatch.await()
            
            // Perform a graceful shutdown
            stopAsync()
        }
    }
    
    /**
     * Asynchronous version of start for use in coroutine contexts.
     * Unlike [start], this method doesn't block the current thread.
     * 
     * This is useful for testing or when integrating with other
     * asynchronous systems.
     */
    suspend fun startAsync() {
        // Only allow starting once
        check(!isRunning) { "App is already running" }
        TraceCollector.registerReporter(ConsoleTraceReporter)
        
        // Initialize the runtime environment
        AppRuntime.initialize()
        
        // Activate this app 
        activate()
    }
    
    /**
     * Stops this application and all its modules.
     * For programmatic shutdown (not usually needed by developers
     * since shutdown is handled automatically via JVM hooks).
     * 
     * When called, this method:
     * 1. Signals the app to begin shutdown
     * 2. Blocks until shutdown is complete
     * 
     * Note: In most cases, you don't need to call this method directly.
     * The app will shut down automatically when the JVM terminates.
     */
    fun stop() {
        // Block until complete
        runBlocking {
            stopAsync()
        }

        // Programmatically request shutdown
        shutdownLatch.countDown()
    }
    
    /**
     * Asynchronous version of stop for use in coroutine contexts.
     * This is useful for testing or when integrating with other
     * asynchronous systems.
     */
    suspend fun stopAsync() {
        if (!isRunning) return

        try {
            // Deactivate this app
            deactivate()
        } catch(_: Exception) {

        } finally {
            // Shut down the runtime environment
            AppRuntime.shutdown()
        }
    }
    
    /**
     * Start the application: install components, activate all modules,
     * and run the didStart hook.
     */
    override suspend fun activate() {
        if (isRunning) return
        activateModules()
        didStartBlock?.invoke()
        isRunning = true
    }

    /**
     * Protected method to allow test subclasses to override activation without
     * dealing with async/await complexities
     */
    protected open suspend fun activateModules() {
        modules.forEach { it.activate() }
    }

    /**
     * Gracefully shutdown the application: run the willStop hook,
     * deactivate all modules in reverse order.
     */
    override suspend fun deactivate() {
        if (!isRunning) return
        // Execute developer hook first
        willStopBlock?.invoke()
        
        // Then deactivate all modules in reverse order
        modules.asReversed().forEach { it.deactivate() }
        isRunning = false
    }
    
    /**
     * Sets up JVM shutdown hooks to handle SIGINT/SIGTERM signals
     * for a graceful application shutdown.
     */
    internal fun setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(Thread {
            println("\n🛑 Received shutdown signal")
            stop()
        })
    }
}