package robot_cleaner.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import runix.dsl.derivedSignal

object PowerState {
    val batteryLevel = MutableStateFlow(100) // %

    val isCharging = MutableStateFlow(false)
    val isDocked = MutableStateFlow(false)

    // Derived: battery is considered low under 20%
    val isBatteryLow: StateFlow<Boolean> = derivedSignal(batteryLevel) { it < 20 }

    // Derived: battery is full when above 95%
    val isBatteryFull: StateFlow<Boolean> = derivedSignal(batteryLevel) { it >= 95 }
}