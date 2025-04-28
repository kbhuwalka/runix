package runix.temporal

import kotlin.time.TimeSource

/**
 * The result of evaluating a temporal condition at a single instant.
 *
 * - [True]: condition is satisfied right now.
 * - [False]: condition is definitively not satisfied.
 * - [Delayed]: condition is not yet satisfied; retry at [nextCheckAt].
 */
internal sealed class ConditionEval {

    /**
     * Inverts this evaluation:
     * - True  → False
     * - False → True
     * - Delayed → Delayed
     */
    internal abstract fun invert(): ConditionEval

    internal companion object {

        /**
         * AND-semantics merge:
         * • If *any* result is False → returns False immediately.
         * • Else if *any* result is Delayed → returns the earliest Delayed.
         * • Else → returns True.
         */
        internal fun mergeAll(results: List<ConditionEval>): ConditionEval {
            var earliest: TimeSource.Monotonic.ValueTimeMark? = null

            for (r in results) {
                when (r) {
                    is False   -> return False
                    is Delayed -> earliest = earliest?.let { minOf(it, r.nextCheckAt) } ?: r.nextCheckAt
                    is True    -> Unit
                }
            }
            return earliest?.let { Delayed(it) } ?: True
        }

        /**
         * OR-semantics merge:
         * • If *any* result is True → returns True immediately.
         * • Else if *any* result is Delayed → returns the earliest Delayed.
         * • Else → returns False.
         */
        internal fun mergeAny(results: List<ConditionEval>): ConditionEval {
            var earliest: TimeSource.Monotonic.ValueTimeMark? = null

            for (r in results) {
                when (r) {
                    is True    -> return True
                    is Delayed -> earliest = earliest?.let { minOf(it, r.nextCheckAt) } ?: r.nextCheckAt
                    is False   -> Unit
                }
            }
            return earliest?.let { Delayed(it) } ?: False
        }
    }

    /** Condition is currently satisfied. */
    object True : ConditionEval() {
        override fun invert() = False
    }

    /** Condition is currently not satisfied. */
    object False : ConditionEval() {
        override fun invert() = True
    }

    /**
     * Condition not yet satisfied; must re-evaluate at [nextCheckAt].
     *
     * @property nextCheckAt timestamp (monotonic) when the engine should retry.
     */
    data class Delayed(val nextCheckAt: TimeSource.Monotonic.ValueTimeMark) : ConditionEval() {
        override fun invert() = this
    }
}