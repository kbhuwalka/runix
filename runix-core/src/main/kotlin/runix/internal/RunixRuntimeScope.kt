package runix.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object RunixRuntimeScope {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
