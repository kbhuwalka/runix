package runix.primitives.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import runix.core.logger
import runix.primitives.Reaction
import runix.primitives.RunixScheduler
import runix.primitives.Signal
import runix.primitives.tracing.ExecutionTrace
import runix.tracing.ExecutionStatus
import runix.tracing.Trace
import runix.tracing.TraceLogger

class SignalRegistry(
    private val scheduler: RunixScheduler,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val traceLogger: TraceLogger? = null
) {
    private val signals = mutableMapOf<String, MutableSharedFlow<Unit>>()
    private val listeners = mutableMapOf<String, MutableList<Reaction>>()

    fun fire(signal: Signal, parentTrace: ExecutionTrace? = null) {
        val name = signal.name
        val subscribedReactions = listeners[name].orEmpty()

        // Create trace for signal emission
        val trace = if (parentTrace != null)
            Trace.child("Signal($name)", parentTrace)
        else
            Trace.root("Signal($name)", scheduler)

        // Log the signal event
        Trace.log(trace, type = "Signal", status = ExecutionStatus.Triggered, logger = traceLogger)

        scope.launch {
            signals[name]?.emit(Unit)
        }

        subscribedReactions.forEach { reaction ->
            scheduler.schedule(reaction, trace)
        }
    }

    fun listen(name: String): MutableSharedFlow<Unit> {
        return signals.getOrPut(name) {
            MutableSharedFlow(extraBufferCapacity = 1)
        }
    }

    fun subscribe(signal: Signal, reaction: Reaction) {
        logger.info { "🔗 Subscribed reaction '${reaction.name}' to signal '${signal.name}'" }
        listeners.getOrPut(signal.name) { mutableListOf() }.add(reaction)
    }

    fun unsubscribe(signal: Signal, reaction: Reaction) {
        listeners[signal.name]?.apply {
            remove(reaction)
            logger.info { "❌ Unsubscribed reaction '${reaction.name}' from signal '${signal.name}'" }
            if (isEmpty()) listeners.remove(signal.name)
        }
    }
}