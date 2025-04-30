package runix.dsl

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import runix.internal.RuntimeScope
import runix.temporal.trackers.TrackerRegistry

/**
 * Creates a derived StateFlow<Boolean> that updates whenever [source] changes,
 * using the provided [transform] function.
 *
 * This flow is tracked with [TrackerRegistry] under the given [key], and is compatible
 * with all temporal expressions like .persistedFor(), .wasStableFor(), etc.
 *
 * Example:
 *     val isHot = derivedSignal(temperature, "is-hot") { it > 85f }
 */
fun <T> derivedStateFlow(
    source: StateFlow<T>,
    transform: (T) -> Boolean
): StateFlow<Boolean> {
    val derived = MutableStateFlow(transform(source.value))

    RuntimeScope.scope.launch {
        source.collect { value ->
            val result = transform(value)
            if (derived.value != result) {
                derived.value = result
            }
        }
    }

    return derived
}
