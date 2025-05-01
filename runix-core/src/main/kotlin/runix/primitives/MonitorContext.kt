package runix.primitives

class MonitorContext private constructor(
    private val base: RunixExecutionContext
) {
    private val scheduler = base.scheduler

    fun emit(signal: Signal) {

        scheduler.fireSignal(signal)
    }

    companion object {
        fun from(ctx: RunixExecutionContext, monitorName: String): MonitorContext {
            return MonitorContext( ctx)
        }
    }
}
