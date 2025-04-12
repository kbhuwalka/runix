package chase_squish.modules

import chase_squish.UI
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import runix.dsl.action
import runix.dsl.derivedStateFlow
import runix.dsl.monitor
import runix.dsl.reaction
import runix.primitives.ActionError
import runix.primitives.ActionResult
import runix.primitives.Monitor
import runix.primitives.Reaction
import runix.primitives.Signal
import runix.temporal.persistedFor
import runix.temporal.whenTrue
import kotlin.time.Duration.Companion.seconds

object BrushModule {
    val runningState = MutableStateFlow(false)
    val currentTemperature = MutableStateFlow(0f)

    object Conditions {
        val isTemperatureAbove80 = derivedStateFlow(currentTemperature) { it > 80 }
        val isTemperatureAbove100 = derivedStateFlow(currentTemperature) { it > 100 }
    }

    object Signals {
        val BrushTemperatureHigh = object : Signal("BrushTemperatureHigh") {}
        val BrushOverheat = object : Signal("BrushOverheat") {}
    }

    object Monitors {
        val TemperatureWarningMonitor = monitor("BrushTemperatureHigh") {
            fireIf(Conditions.isTemperatureAbove80.whenTrue())
            emits(Signals.BrushTemperatureHigh)
        }

        val BrushOverheatMonitor = monitor("BrushOverheat") {
            fireIf(Conditions.isTemperatureAbove100.persistedFor(3.seconds))
            emits(Signals.BrushOverheat)
        }
    }

    object Reactions {
        val brushOverheat = reaction("BrushOverheat") {
            on(Signals.BrushOverheat)
            run { context ->
                context.schedule(Actions.stopBrushes)
            }
        }
    }

    object Actions {
        val startBrushes = action("StartBrushes") {
            onExecute {
                println("🧹 Starting brushes")
                if (currentTemperature.value > 80f) {
                    fireSignal(UI.Notifications.BrushTemperatureTooHighToStart)
                    return@onExecute ActionResult.Failure(ActionError("StartBrushesFailed", "The brushes were too hot to start"))
                } else {
                    try {
                        delay(500) // Start module
                        runningState.value = true
                        return@onExecute ActionResult.Success("Brushes started")
                    } catch (error: Error) {
                        return@onExecute ActionResult.Failure(ActionError("StartBrushesFailed", "The module failed to start"))
                    }
                }
            }
        }
        val stopBrushes = action("StopBrushes") {
            onExecute {
                println("🧹 Stopping brushes")
                if (runningState.value == false) {
                    return@onExecute ActionResult.Failure(ActionError("StopBrushesFailed", "The brushes are not running"))
                }

                try {
                    delay(200) // Stop module
                    runningState.value = false
                    return@onExecute ActionResult.Success("Brushes started")
                } catch (error: Error) {
                    return@onExecute ActionResult.Failure(ActionError("StartBrushesFailed", "The module failed to stop"))
                }
            }
        }
    }

    fun allReactions(): List<Reaction> = listOf(
        Reactions.brushOverheat
    )

    fun allMonitors(): List<Monitor> = listOf(
        Monitors.TemperatureWarningMonitor,
        Monitors.BrushOverheatMonitor
    )
}
