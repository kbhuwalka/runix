package runix.tools

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import runix.tracing.InstantSerializer
import runix.tracing.TraceLogEntry
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

        println("🧠 Execution Timeline from file: ${file.name}")
        println("=".repeat(100))
        println("  Time  | Type     | Status     | Duration | Name / Path")
        println("-".repeat(100))

        for (entry in entries) {
            val timeDelta = Duration.between(baseTime, entry.timestamp).toMillis()
            val duration = if (entry.durationMs > 0) "${entry.durationMs}ms" else "-"
            val path = entry.tracePath.joinToString(" → ")

            println(
                "%6sms | %-8s | %-10s | %-8s | %s".format(
                    timeDelta,
                    entry.type,
                    entry.status,
                    duration,
                    path
                )
            )
        }

        println("=".repeat(100))
    }
}