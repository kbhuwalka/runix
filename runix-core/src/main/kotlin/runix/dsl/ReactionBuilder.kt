package runix.dsl

import runix.primitives.Reaction
import runix.primitives.ReactionContext
import runix.primitives.Signal

class ReactionBuilder(private val name: String) {
    private val signals = mutableListOf<Signal>()
    private lateinit var handler: (suspend (ctx: ReactionContext) -> Unit)

    fun on(vararg signals: Signal) {
        this.signals += signals
    }

    fun run(block: suspend (ctx: ReactionContext) -> Unit) {
        this.handler = block
    }

    fun build(): Reaction {
        require(signals.isNotEmpty()) { "Reaction '$name' must define at least one signal to react to." }

        return object : Reaction(
            name = "Reaction($name)",
            signalNames = signals
        ) {
            override suspend fun onFired(context: ReactionContext) {
                handler(context)
            }
        }
    }
}

fun reaction(name: String, block: ReactionBuilder.() -> Unit): Reaction {
    return ReactionBuilder(name).apply(block).build()
}
