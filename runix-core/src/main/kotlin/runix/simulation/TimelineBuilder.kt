package runix.simulation

import kotlinx.coroutines.flow.MutableStateFlow

class TimelineBuilder {
    private val schedules = mutableListOf<SignalSchedule<*>>()

    fun <T> forSignal(
        flow: MutableStateFlow<T>,
        name: String,
        block: SignalSchedule<T>.() -> Unit
    ) {
        val schedule = SignalSchedule(name, flow).apply(block)
        schedules.add(schedule)
    }

    internal fun allChanges(): List<ScheduledChange<*>> =
        schedules.flatMap { it.getSchedule() }
}