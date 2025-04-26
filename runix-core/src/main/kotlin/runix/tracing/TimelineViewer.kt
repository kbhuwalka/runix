package runix.tracing

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.io.File
import java.time.Duration
import java.time.Instant

object TimelineViewer {

    private val json = Json {
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            contextual(Instant::class, InstantSerializer)
        }
    }

    private fun color(text: String, ansi: String): String = "$ansi$text\u001B[0m"

    private fun statusColor(status: String): String = when (status.uppercase()) {
        "SUCCESS", "OK" -> color(status, "\u001B[32m") // green
        "FAILED", "ERROR" -> color(status, "\u001B[31m") // red
        "RUNNING" -> color(status, "\u001B[34m") // blue
        else -> status
    }

    private fun typeColor(type: String): String = when {
        type.contains("ACTION", true) -> color(type, "\u001B[36m") // cyan
        type.contains("MONITOR", true) -> color(type, "\u001B[35m") // magenta
        type.contains("REACTION", true) -> color(type, "\u001B[33m") // yellow
        else -> type
    }

    fun printTimeline(file: File) {
        val entries = file
            .readLines()
            .mapNotNull { line ->
                try {
                    json.decodeFromString<TraceLogEntry>(line)
                } catch (e: Exception) {
                    println("⚠️ Could not parse: $line")
                    null
                }
            }
            .sortedBy { it.timestamp }

        if (entries.isEmpty()) {
            println("❌ No valid entries found in ${file.name}")
            return
        }

        val baseTime = entries.first().timestamp

        println("🧠 Execution Timeline from: ${file.name}")
        println("=".repeat(90))
        println("  Time  | Type        | Status     | Duration | Path")
        println("-".repeat(90))

        for (entry in entries) {
            val timeDelta = Duration.between(baseTime, entry.timestamp).toMillis()
            val duration = if (entry.durationMs > 0) "${entry.durationMs}ms" else "-"
            val path = entry.tracePath.joinToString(" → ")
            val type = typeColor(entry.type.padEnd(11))
            val status = statusColor(entry.status.name.padEnd(10))

            println(String.format("%6sms | %s | %s | %8s | %s", timeDelta, type, status, duration, path))
        }

        println("=".repeat(90))
        println("✅ Total entries: ${entries.size}")
    }
}