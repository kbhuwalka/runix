package runix.tracing

interface TraceLogger {
    fun log(entry: TraceLogEntry)
}