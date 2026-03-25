package examples.examples.basic1

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import runix.primitives.action.ActionResult
import runix.primitives.action.action
import runix.primitives.module.AppModule
import runix.primitives.monitor.monitor
import runix.primitives.reaction.reaction
import runix.primitives.signal.signal
import runix.temporal.dsl.anyOf
import runix.temporal.dsl.hasBeenTrueFor
import runix.temporal.dsl.hasFluctuated
import runix.temporal.dsl.not
import kotlin.time.Duration.Companion.seconds

val flip = MutableStateFlow(false)

val signal = signal<Unit>("FlipSignal")

val flipperMonitor = monitor("flipper") {
    anyOf(
        flip.hasBeenTrueFor(1.seconds),
        not(flip.hasBeenTrueFor(1.seconds))
    )
} emits signal

val reaction = reaction("ReactionToFlip", signal) {
    delay(1.seconds)
    flip.value = !flip.value
    action.run(Unit)
}

val action = action("ActionToFlip") {
    signal.emit(Unit)
    println("emitted signal")
    delay(1.seconds)
    println("delayed")
    ActionResult.Success
}

object BasicModule: AppModule("BasicModule") {
    init {
        defineBehavior {
            +flipperMonitor
            +reaction
            +action
        }

        didStart {
            println("Starting BasicModule")
            flip.value = !flip.value
        }

        willStop {
            println("Stopping BasicModule")
        }
    }
}