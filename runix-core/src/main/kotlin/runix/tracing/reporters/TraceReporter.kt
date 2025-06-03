package runix.tracing.reporters

import runix.tracing.events.TraceEvent

/**
 * A TraceReporter is responsible for handling the output of trace events.
 * Implementations might log to the console, write to a file, or stream to a dashboard.
 */
internal fun interface TraceReporter {
    fun report(event: TraceEvent)
}
