package runix.primitives
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Common interface for all named flows
interface IRunixFlow<T> {
    val name: String
    val value: T
    val flow: StateFlow<T>
}

// Read-only version
class RunixFlow<T>(
    override val name: String,
    override val flow: StateFlow<T>
) : IRunixFlow<T> {
    override val value: T get() = flow.value

    override fun toString(): String = "$name=$value"
}

// Mutable version
class MutableRunixFlow<T>(
    override val name: String,
    private val _flow: MutableStateFlow<T>
) : IRunixFlow<T> {

    override val flow: StateFlow<T> get() = _flow

    override var value: T
        get() = _flow.value
        set(value) {
            _flow.value = value
        }

    override fun toString(): String = "$name=$value"
}

fun <T> mutableRunixFlow(name: String, initial: T): MutableRunixFlow<T> {
    return MutableRunixFlow(name, MutableStateFlow(initial))
}

fun <T> runixFlow(name: String, flow: StateFlow<T>): RunixFlow<T> {
    return RunixFlow(name, flow)
}