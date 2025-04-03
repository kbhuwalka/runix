package runix.memory

import java.time.Instant

sealed class ConditionEval {
    object True : ConditionEval()
    object False : ConditionEval()
    data class Delayed(val nextCheckAt: Instant) : ConditionEval()
}