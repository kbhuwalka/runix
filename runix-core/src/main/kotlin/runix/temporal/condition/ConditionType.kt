package runix.temporal.condition

import runix.temporal.trackers.TrackerProvider
import kotlin.time.Duration

internal sealed interface ConditionType {
    val retention: Duration
    fun compileExpression(
        key: String,
        provider: TrackerProvider
    ): TemporalExpression
}
