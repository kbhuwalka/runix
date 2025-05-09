package runix.temporal

import kotlinx.coroutines.flow.StateFlow
import runix.temporal.trackers.BaseTracker
import kotlin.time.Duration

internal data class FlowBinding<T>(
    val key: String,
    val retention: Duration,
    val sourceFlow: StateFlow<T>,
    val tracker: BaseTracker<T>
)
