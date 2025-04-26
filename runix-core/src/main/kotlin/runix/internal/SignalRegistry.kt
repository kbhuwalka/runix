package runix.internal

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import runix.core.RunixScheduler
import runix.core.logger
import runix.primitives.Reaction
import runix.primitives.Signal
import runix.tracing.ExecutionStatus
import runix.tracing.ExecutionTrace
import runix.tracing.Trace
import runix.tracing.TraceLogger
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

typealias CancellationCallback = (ExecutionTrace) -> Unit

internal class SignalRegistry(
    private val scheduler: RunixScheduler,
    private val traceLogger: TraceLogger? = null
) {
    private val signals = ConcurrentHashMap<String, MutableSharedFlow<Unit>>()
    private val listeners = ConcurrentHashMap<String, CopyOnWriteArrayList<Reaction>>()
    private val cancellationListeners = ConcurrentHashMap<String, CopyOnWriteArrayList<CancellationCallback>>()
    private val lastSignalTraces = ConcurrentHashMap<String, ExecutionTrace>()

    fun fire(signal: Signal, parentTrace: ExecutionTrace? = null) {
        val name = signal.name
        val subscribedReactions = listeners[name].orEmpty()
        val cancelCallbacks = cancellationListeners[name].orEmpty()

        // Create trace for signal emission
        val trace = if (parentTrace != null) {
            Trace.child("Fired", parentTrace, actor = signal.actor, tags = signal.tags)
        } else {
            Trace.root("Signal($name)", scheduler, actor = signal.actor, tags = signal.tags)
        }

        // Log the signal event
        Trace.log(
            trace,
            type = "Monitor",
            status = ExecutionStatus.Triggered,
            logger = traceLogger,
            actor = signal.actor,
            tags = signal.tags,
            startTime = trace.startTime,
            endTime = Instant.now()
        )

        lastSignalTraces[name] = trace

        RunixRuntimeScope.scope.launch {
            signals[name]?.emit(Unit)
        }

        // Fire reactions
        subscribedReactions.forEach { reaction ->
            scheduler.run(reaction, trace)
        }

        // Fire cancellation listeners
        cancelCallbacks.forEach { callback ->
            callback(trace)
        }
    }

    fun listen(name: String): MutableSharedFlow<Unit> {
        return signals.getOrPut(name) {
            MutableSharedFlow(extraBufferCapacity = 1)
        }
    }

    fun subscribe(signal: Signal, reaction: Reaction) {
        logger.info { "🔗 Subscribed reaction '${reaction.name}' to signal '${signal.name}'" }
        listeners.getOrPut(signal.name) { CopyOnWriteArrayList() }.add(reaction)
    }

    fun unsubscribe(signal: Signal, reaction: Reaction) {
        listeners[signal.name]?.apply {
            remove(reaction)
            logger.info { "❌ Unsubscribed reaction '${reaction.name}' from signal '${signal.name}'" }
            if (isEmpty()) listeners.remove(signal.name)
        }
    }

    fun registerCancellationListener(signal: Signal, callback: CancellationCallback) {
        cancellationListeners.getOrPut(signal.name) { CopyOnWriteArrayList() }.add(callback)
    }

    fun unregisterCancellationListener(signal: Signal, callback: CancellationCallback) {
        cancellationListeners[signal.name]?.apply {
            remove(callback)
            if (isEmpty()) cancellationListeners.remove(signal.name)
        }
    }

    fun lastSignalTrace(signalName: String): ExecutionTrace? {
        return lastSignalTraces[signalName]
    }

    internal fun logSkipped(
        name: String,
        reason: String,
        actor: String? = null,
        tags: List<String> = emptyList(),
        startTime: Instant = Instant.now(),
        endTime: Instant = Instant.now()
    ) {
        val trace = Trace.root("Skipped($name)", scheduler, actor = actor, tags = tags)
        Trace.log(
            trace,
            type = "Action",
            status = ExecutionStatus.Skipped,
            logger = traceLogger,
            message = reason,
            actor = actor,
            tags = tags,
            startTime = startTime,
            endTime = endTime
        )
    }
}
