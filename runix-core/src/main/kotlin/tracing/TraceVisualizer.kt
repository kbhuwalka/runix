package runix.tracing.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import runix.tracing.InstantSerializer
import runix.tracing.TraceLogEntry
import java.io.File
import java.time.Instant

object TraceVisualizer {

    fun generateFrom(logFile: File, outputDir: File) {
        val json = Json {
            ignoreUnknownKeys = true
            serializersModule = SerializersModule {
                contextual(Instant::class, InstantSerializer)
            }
        }
        val entries = logFile.readLines()
            .filter { it.isNotBlank() }
            .map { json.decodeFromString<TraceLogEntry>(it) }

        val groupedByRoot = entries.groupBy { entry ->
            entry.tracePath.firstOrNull() ?: "unknown"
        }

        outputDir.mkdirs()

        for ((root, group) in groupedByRoot) {
            val file = File(outputDir, "$root.mmd")
            val mermaid = generateMermaid(group)
            file.writeText(mermaid)
        }
    }

    private fun generateMermaid(entries: List<TraceLogEntry>): String {
        val nodes = mutableMapOf<Long, String>()
        val edges = mutableListOf<Pair<Long, Long>>()

        for (entry in entries) {
            nodes[entry.id] = "${entry.type}:${entry.name} [${entry.status}]"
            if (entry.parentId != null) {
                edges.add(entry.parentId to entry.id)
            }
        }

        val builder = StringBuilder()
        builder.appendLine("```mermaid")
        builder.appendLine("graph TD")

        for ((id, label) in nodes) {
            val cleanLabel = label.replace("\"", "")
            builder.appendLine("    $id[\"$cleanLabel\"]")
        }

        for ((from, to) in edges) {
            builder.appendLine("    $from --> $to")
        }

        builder.appendLine("```")
        return builder.toString()
    }
}