package runix.temporal.condition

import kotlin.time.ComparableTimeMark

internal typealias TemporalExpression = () -> ConditionEval

/**
 * A runtime evaluation result for a declarative condition.
 *
 * This result is returned by evaluating a compiled [runix.temporal.MonitoredCondition].
 *
 * - [True]: The condition is currently satisfied.
 * - [False]: The condition is definitively not satisfied.
 * - [Delayed]: The condition is not yet satisfied, but may become satisfied later.
 *              Check again after [nextCheckAt].
 */
internal sealed class ConditionEval {

    /**
     * Returns the logical inverse of this evaluation:
     * - `True` becomes `False`
     * - `False` becomes `True`
     * - `Delayed` remains unchanged (cannot negate future readiness)
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
            var earliest: ComparableTimeMark? = null

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
            var earliest: ComparableTimeMark? = null

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
    data class Delayed(val nextCheckAt: ComparableTimeMark) : ConditionEval() {
        override fun invert() = this
    }
}