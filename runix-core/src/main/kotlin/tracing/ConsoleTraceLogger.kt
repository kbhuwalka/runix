package runix.tracing

class ConsoleTraceLogger : TraceLogger {
    override fun log(entry: TraceLogEntry) {
        val prefix = when (entry.type) {
            "Action" -> "⚙️ "
            "Reaction" -> "🔁"
            "Signal" -> "⚡"
            else -> "📄"
        }

        println(
            "$prefix [${entry.type}] ${entry.name} — ${entry.status} (${entry.durationMs}ms)"
                    + "\n  ➜ Trace: ${entry.tracePath.joinToString(" → ")}"
        )
    }
}