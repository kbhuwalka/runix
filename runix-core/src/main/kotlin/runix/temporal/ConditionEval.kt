package runix.temporal

import kotlin.time.TimeSource

sealed class ConditionEval {
    object True : ConditionEval()
    object False : ConditionEval()
    data class Delayed(val nextCheckAt: TimeSource.Monotonic.ValueTimeMark) : ConditionEval()
}
