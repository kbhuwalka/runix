package runix.tracing

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.io.File
import java.io.FileWriter
import java.time.Instant
import java.time.format.DateTimeFormatter

class FileTraceLogger(private val outputDir: File) : TraceLogger {

    private val json = Json {
        encodeDefaults = true
        prettyPrint = false
        serializersModule = SerializersModule {
            contextual(Instant::class, InstantSerializer)
        }
    }
    private val logFile = run {
        outputDir.mkdirs()
        val safeName = "trace-log-${timestamp()}.jsonl"
        val file = File(outputDir, safeName)

        // ✅ Ensure the file exists
        if (!file.exists()) {
            file.createNewFile()
        }

        file
    }

    init {
        outputDir.mkdirs()
        if (!logFile.exists()) logFile.createNewFile()
    }

    override fun log(entry: TraceLogEntry) {
        FileWriter(logFile, true).use { writer ->
            writer.write(json.encodeToString(entry))
            writer.write("\n")
        }
    }

    fun getLogFile(): File = logFile

    private fun timestamp(): String {
        return DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            .replace(":", "-")
    }
}

object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.parse(decoder.decodeString())
    }
}
