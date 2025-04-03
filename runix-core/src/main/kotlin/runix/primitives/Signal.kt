package runix.primitives

import kotlin.time.Duration

abstract class Signal(val name: String) {

    // Optional: Whether this signal should be allowed to fire
    open fun shouldFire(): Boolean = true

    // Optional metadata (can be used for grouping or filtering)
    open val category: String = "default"

    // Optional throttling interval (used by SignalRegistry if supported)
    open val throttleInterval: Duration? = null

    override fun toString(): String = name
}
