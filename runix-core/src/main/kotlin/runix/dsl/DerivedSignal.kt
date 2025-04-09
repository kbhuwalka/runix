package runix.dsl

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import runix.internal.RunixRuntimeScope
import runix.temporal.TemporalEngine

/**
 * Creates a derived StateFlow<Boolean> that updates whenever [source] changes,
 * using the provided [transform] function.
 *
 * This flow is tracked with [TemporalEngine] under the given [key], and is compatible
 * with all temporal expressions like .persistedFor(), .wasStableFor(), etc.
 *
 * Example:
 *     val isHot = derivedSignal(temperature, "is-hot") { it > 85f }
 */
fun <T> derivedSignal(
    source: StateFlow<T>,
    transform: (T) -> Boolean
): StateFlow<Boolean> {
    val derived = MutableStateFlow(transform(source.value))

    RunixRuntimeScope.scope.launch {
        source.collect { value ->
            derived.value = transform(value)
        }
    }

    return derived
}