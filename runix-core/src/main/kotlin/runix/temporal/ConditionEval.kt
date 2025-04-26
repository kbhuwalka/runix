package runix.temporal

import kotlin.time.TimeSource

sealed class ConditionEval {
    abstract fun invert(): ConditionEval

    companion object {
        fun mergeAll(results: List<ConditionEval>): ConditionEval {
            var earliestDelay: TimeSource.Monotonic.ValueTimeMark? = null

            for (result in results) {
                when (result) {
                    is True -> { /* keep going */ }
                    is False -> return False
                    is Delayed -> {
                        if (earliestDelay == null || result.nextCheckAt < earliestDelay) {
                            earliestDelay = result.nextCheckAt
                        }
                    }
                }
            }

            return earliestDelay?.let { Delayed(it) } ?: True
        }

        fun mergeAny(results: List<ConditionEval>): ConditionEval {
            var earliestDelay: TimeSource.Monotonic.ValueTimeMark? = null

            for (result in results) {
                when (result) {
                    is True -> return ConditionEval.True
                    is False -> { /* keep going */ }
                    is Delayed -> {
                        if (earliestDelay == null || result.nextCheckAt < earliestDelay) {
                            earliestDelay = result.nextCheckAt
                        }
                    }
                }
            }

            return earliestDelay?.let { Delayed(it) } ?: False
        }
    }

    object True : ConditionEval() {
        override fun invert() = False
    }

    object False : ConditionEval() {
        override fun invert() = True
    }

    data class Delayed(val nextCheckAt: TimeSource.Monotonic.ValueTimeMark) : ConditionEval() {
        override fun invert() = this
    }
}
