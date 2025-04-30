package runix.dsl

import kotlinx.coroutines.flow.StateFlow

/**
 * A wrapper around [StateFlow] that associates a human-readable [label] for traceability,
 * introspection, and debugging. Used within monitors to enhance log visibility and
 * cognitive condition tracking.
 *
 * Labels are optional but recommended for improved trace diagnostics.
 *
 * @param flow The original [StateFlow] being monitored
 * @param label A developer-provided name used for trace and condition labeling
 */
data class LabeledFlow<T>(
    val flow: StateFlow<T>,
    val label: String
)


/**
 * Associates a human-readable [name] with this [StateFlow], returning a [LabeledFlow]
 * for use in condition tracing, logs, and introspection.
 *
 * Use this when:
 * - You want to improve trace clarity for monitors using this signal
 * - You want logs to include meaningful signal names
 *
 * This does not alter signal behavior — it only affects labeling.
 *
 * Example:
 * ```
 * val motor = motorFlow.label("motor.isRunning")
 * ```
 *
 * @param name The label to associate with this flow
 * @return A labeled wrapper of the current [StateFlow]
 */
fun <T> StateFlow<T>.label(name: String): LabeledFlow<T> =
    LabeledFlow(this, name)