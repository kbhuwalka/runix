package runix.tracing.reporters

import org.slf4j.LoggerFactory
import runix.tracing.events.TraceEvent

/**
 * A basic TraceReporter that logs all events to the console using structured logging.
 * Primarily used in development and local testing environments.
 */
object ConsoleTraceReporter : TraceReporter {
    private val logger = LoggerFactory.getLogger("TraceReporter")

    override fun report(event: TraceEvent) {
        logger.info("[{}] {} :: {}", event.timestamp, event.javaClass.simpleName, event)
    }
}
