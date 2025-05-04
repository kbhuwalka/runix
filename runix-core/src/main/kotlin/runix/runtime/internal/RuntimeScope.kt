package runix.runtime.internal

import kotlinx.coroutines.CoroutineScope
import java.util.concurrent.atomic.AtomicReference

/**
 * Singleton accessor for the coroutine scope associated with the currently running app.
 * This is used internally by primitives (e.g. Actions) to launch long-lived coroutines
 * without requiring developers to pass scopes explicitly.
 *
 * The scope must be set during AppRuntime startup and cleared during shutdown.
 */
internal object RuntimeScope {
    private val activeScope = AtomicReference<CoroutineScope?>()

    /**
     * Returns the active [CoroutineScope] used by all runtime operations.
     *
     * Throws if the app has not started or the scope was not initialized.
     */
    val scope: CoroutineScope
        get() = activeScope.get()
            ?: error("RuntimeScope not initialized. Has AppRuntime.start() been called?")

    /**
     * Installs a [CoroutineScope] to be used across the runtime.
     * This should only be called by [AppRuntime].
     */
    fun install(scope: CoroutineScope) {
        check(activeScope.get() == null) {
            "RuntimeScope is already installed."
        }
        activeScope.set(scope)
    }

    /**
     * Clears the active scope. After this, [scope] access will throw.
     */
    fun clear() {
        activeScope.set(null)
    }
}