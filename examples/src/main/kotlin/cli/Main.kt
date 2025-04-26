package cli

import runix.tracing.TimelineViewer
import java.io.File

fun main(args: Array<String>) {
    val logFile = File("logs/trace-log-2025-04-19T18-08-57.189105Z.jsonl")
    if (!logFile.exists()) {
        println("❌ File not found: ${args[1]}")
        return
    }

    TimelineViewer.printTimeline(logFile)
}