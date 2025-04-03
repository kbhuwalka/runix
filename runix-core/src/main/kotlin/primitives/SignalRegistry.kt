package runix.primitives.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import runix.primitives.Reaction
import runix.primitives.RunixJob
import runix.primitives.RunixScheduler
import runix.primitives.Signal
import runix.primitives.tracing.ExecutionTrace
import runix.primitives.tracing.child
import runix.tracing.ExecutionStatus
import runix.tracing.ExecutionTimer
import runix.tracing.TraceLogEntry
import runix.tracing.TraceLogger
import java.time.Instant

class SignalRegistry(
    private val scheduler: RunixScheduler,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val logger: TraceLogger? = null
) {
    private val signals = mutableMapOf<String, MutableSharedFlow<Unit>>()
    private val listeners = mutableMapOf<String, MutableList<Reaction>>()

    fun fire(signal: Signal, parentTrace: ExecutionTrace? = null) {
        val name = signal.name
        val subscribedReactions = listeners[name].orEmpty()

        // Create trace for signal emission
        val trace = ExecutionTrace(
            path = (parentTrace?.path ?: emptyList()) + "Signal($name)",
            parentId = parentTrace?.id,
            scheduler = scheduler
        )

        // Log the signal event
        logger?.log(
            TraceLogEntry(
                id = trace.id,
                parentId = trace.parentId,
                type = "Signal",
                name = name,
                timestamp = Instant.now(),
                durationMs = 0,
                status = ExecutionStatus.Triggered,
                tracePath = trace.path
            )
        )

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
        listeners.getOrPut(signal.name) { mutableListOf() }.add(reaction)
    }
}