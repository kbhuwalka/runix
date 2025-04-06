package runix.primitives

import kotlin.time.Duration

abstract class Signal(val name: String) {
    // Optional throttling interval (used by SignalRegistry if supported)
    open val throttleInterval: Duration? = null

    open val actor: String? = this::class.simpleName
    open val tags: List<String> = emptyList()

    override fun toString(): String = name
}
