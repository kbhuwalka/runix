package runix.runtime.internal

import java.util.concurrent.atomic.AtomicBoolean
import runix.primitives.module.AppModule

/**
 * Internal helper to track registration state for Registerables.
 */
internal class RegistrationGuard {
    private val registered = AtomicBoolean(false)
    private var owner: AppModule? = null

    fun register(owner: AppModule) {
        check(registered.compareAndSet(false, true)) {
            "Already registered by module '${this.owner?.name}'"
        }
        this.owner = owner
    }

    fun isRegistered(): Boolean = registered.get()

    fun ownerName(): String? = owner?.name
}