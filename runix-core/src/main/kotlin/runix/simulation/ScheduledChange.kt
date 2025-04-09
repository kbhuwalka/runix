package runix.simulation

data class ScheduledChange<T>(
    val time: Long,
    val value: T,
    val name: String,
    val apply: () -> Unit
)