package runix.primitives.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import runix.primitives.Reaction
import runix.primitives.RunixJob
import runix.primitives.RunixScheduler
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

    fun fire(name: String) {
        val subscribedReactions = listeners[name].orEmpty()

        val trace = ExecutionTrace(
            path = listOf("Signal($name)"),
            scheduler = scheduler
        )

        // Log the signal event
        logger?.log(
            TraceLogEntry(
                id = trace.id,
                parentId = null,
                type = "Signal",
                name = name,
                timestamp = Instant.now(),
                durationMs = 0,
                status = ExecutionStatus.Triggered,
                tracePath = trace.path
            )
        )

        // Schedule each reaction with a child trace
        subscribedReactions.forEach { reaction ->
            val childTrace = trace.child(reaction.name)
            scheduler.schedule(RunixJob(reaction, childTrace))
        }

        // Still emit for any dev-facing listeners
        signals[name]?.let { flow ->
            scope.launch { flow.emit(Unit) }
        }
    }

    fun listen(name: String): MutableSharedFlow<Unit> {
        return signals.getOrPut(name) {
            MutableSharedFlow(extraBufferCapacity = 1)
        }
    }

    fun subscribe(signalName: String, reaction: Reaction) {
        listeners.getOrPut(signalName) { mutableListOf() }.add(reaction)
    }
}