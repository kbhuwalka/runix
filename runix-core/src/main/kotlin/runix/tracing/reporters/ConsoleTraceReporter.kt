package runix.tracing.reporters

import org.slf4j.LoggerFactory
import runix.tracing.events.TraceEvent

/**
 * A basic TraceReporter that logs all events to the console using structured logging.
 * Primarily used in development and local testing environments.
 */
internal object ConsoleTraceReporter : TraceReporter {
    private val logger = LoggerFactory.getLogger("TraceReporter")

    override fun report(event: TraceEvent) {
        logger.info(buildChain(event))
    }

    private fun buildChain(event: TraceEvent): String {
        // Start with the current event
        val eventStr = "${event}(${event.traceId.toString().take(8)})"

        if (event.parent == null) {
            return eventStr
        }

        return "$eventStr <- ${buildChain(event.parent!!)}"
    }

}
