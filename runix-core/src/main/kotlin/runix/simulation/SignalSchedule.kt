package runix.simulation

import kotlinx.coroutines.flow.MutableStateFlow

class SignalSchedule<T>(
    val name: String,
    val flow: MutableStateFlow<T>
) {
    private val changes = mutableListOf<ScheduledChange<T>>()

    fun at(time: Long, value: T) {
        changes.add(ScheduledChange(
            time = time,
            value = value,
            name = name,
            apply = { flow.value = value }
        ))
    }

    fun every(interval: Long, value: T, duration: Long) {
        var t = 0L
        while (t <= duration) {
            at(t, value)
            t += interval
        }
    }

    fun getSchedule(): List<ScheduledChange<T>> = changes
}