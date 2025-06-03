package runix.tracing

import runix.tracing.events.TraceEvent
import runix.tracing.reporters.TraceReporter
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList

/**
 * TraceCollector is the central hub for emitting, storing, and forwarding
 * all structured trace events within the Runix runtime.
 *
 * It is designed to be thread-safe and coroutine-friendly, ensuring high-throughput,
 * low-contention event dispatch in concurrent environments.
 *
 * Key responsibilities:
 * - Accept trace events from any part of the system via `emit()`
 * - Maintain a bounded in-memory buffer of recent events for inspection or replay
 * - Dispatch trace events to one or more registered TraceReporters (e.g., console, JSON)
 *
 * This class avoids explicit locking by using concurrent data structures.
 */
internal object TraceCollector {

    /** Maximum number of trace events retained in the in-memory buffer. */
    private const val MAX_BUFFER_SIZE = 5000

    /** FIFO buffer holding the most recent trace events (thread-safe). */
    private val buffer = ConcurrentLinkedQueue<TraceEvent>()

    /** List of registered trace reporters to which events will be dispatched. */
    private val reporters = CopyOnWriteArrayList<TraceReporter>()

    /**
     * Emits a structured trace event.
     * This method is non-blocking and safe to call from multiple threads or coroutines.
     *
     * @param event The trace event to record and forward.
     */
    fun emit(event: TraceEvent) {
        buffer.add(event)
        trimBufferIfNeeded()
        reporters.forEach { it.report(event) }
    }

    /**
     * Registers a new reporter to receive all future trace events.
     * Reporters are dispatched to synchronously in the order registered.
     *
     * @param reporter The trace reporter to register.
     */
    fun registerReporter(reporter: TraceReporter) {
        reporters.add(reporter)
    }

    /**
     * Returns a snapshot of the current in-memory trace buffer.
     * This is safe for testing or UI inspection.
     *
     * @return A list of recent trace events in FIFO order.
     */
    fun getBufferedEvents(): List<TraceEvent> = buffer.toList()

    /**
     * Clears the trace buffer and registered reporters.
     * This is intended for test resets or full system reinitialization.
     */
    fun reset() {
        buffer.clear()
        reporters.clear()
    }

    /**
     * Ensures that the buffer does not exceed the configured maximum size.
     * Oldest events are dropped first.
     */
    private fun trimBufferIfNeeded() {
        while (buffer.size > MAX_BUFFER_SIZE) {
            buffer.poll()
        }
    }
}
