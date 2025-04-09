package examples.signal_fanout

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import runix.core.RunixScheduler
import runix.dsl.derivedSignal
import runix.dsl.monitor
import runix.dsl.reaction
import runix.primitives.Monitor
import runix.primitives.Reaction
import runix.primitives.Signal
import runix.temporal.asCondition

class SignalFanoutApp {
    // Input state
    val batteryLevel = MutableStateFlow(100)         // Integer %
    val isCharging = MutableStateFlow(false)
    val isDocked = MutableStateFlow(false)

    // Derived boolean
    private val isBatteryLow: StateFlow<Boolean> = derivedSignal(batteryLevel) { it < 20 }

    // Signals
    private val lowBatterySignal = object : Signal("lowBatteryDetected") {}
    private val returnToBase = object : Signal("startReturnToBase") {}
    private val dimLights = object : Signal("dimLights") {}

    // Monitor
    private val monitor = monitor("LowBatteryMonitor") {
        dependsOn(isBatteryLow)
        fireIf(isBatteryLow.asCondition("low-battery"))
        emits(lowBatterySignal)
    }

    // Reactions
    private val returnReaction: Reaction = reaction("ReturnToBaseReaction") {
        on(lowBatterySignal)
        run { context ->
            if (!isCharging.value && !isDocked.value) {
                logger.info("🚗 Battery low, not charging or docked — returning to base")
                context.fire(returnToBase)
            } else {
                logger.info("⚡ Battery low but already docked or charging — skipping return")
            }
        }
    }

    private val dimReaction: Reaction = reaction("DimLightsReaction") {
        on(lowBatterySignal)
        run { context ->
            logger.info("💡 Dimming lights to conserve power")
            context.fire(dimLights)
        }
    }

    fun registerWith(scheduler: RunixScheduler) {
        scheduler.register(monitor)
        scheduler.register(returnReaction)
        scheduler.register(dimReaction)
    }
}