package runix.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object RuntimeScope {

    private var testScopeOverride: CoroutineScope? = null

    private val defaultScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val scope: CoroutineScope
        get() = testScopeOverride ?: defaultScope

    internal fun overrideScopeForTesting(scope: CoroutineScope) {
        testScopeOverride = scope
    }

    internal fun resetScope() {
        testScopeOverride = null
    }
}