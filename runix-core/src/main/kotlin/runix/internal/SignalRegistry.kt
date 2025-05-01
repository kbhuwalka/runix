package runix.internal

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import runix.core.RunixScheduler
import runix.core.logger
import runix.primitives.Reaction
import runix.primitives.Signal
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList


internal class SignalRegistry(
    private val scheduler: RunixScheduler,
) {
    private val signals = ConcurrentHashMap<String, MutableSharedFlow<Unit>>()
    private val listeners = ConcurrentHashMap<String, CopyOnWriteArrayList<Reaction>>()

    fun fire(signal: Signal) {
        val name = signal.name
        val subscribedReactions = listeners[name].orEmpty()

        RuntimeScope.scope.launch {
            signals[name]?.emit(Unit)
        }

        // Fire reactions
        subscribedReactions.forEach { reaction ->
            scheduler.run(reaction)
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
}
