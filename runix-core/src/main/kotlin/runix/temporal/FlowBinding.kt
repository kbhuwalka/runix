package runix.temporal

import runix.temporal.trackers.BaseTracker
import kotlin.time.Duration

internal data class FlowBinding<T>(
    val key: String,
    val retention: Duration,
    val createTracker: (onUpdate: () -> Unit) -> BaseTracker<T>
)