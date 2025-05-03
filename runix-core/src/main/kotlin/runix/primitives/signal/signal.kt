package runix.primitives.signal

/**
 * Defines a new signal with a developer-defined [name].
 *
 * Used in top-level declarations:
 * ```
 * val batteryLow = signal<Unit>("batteryLow")
 * ```
 *
 * @param T The type of data emitted by the signal
 * @return A new [SignalHandle] that can be wired into behavior modules
 */
fun <T> signal(name: String): SignalHandle<T> = SignalHandle(name)