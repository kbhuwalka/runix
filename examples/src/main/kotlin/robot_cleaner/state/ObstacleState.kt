package robot_cleaner.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import runix.dsl.derivedStateFlow

object ObstacleState {
    val obstacleDetected = MutableStateFlow(false)
    val isWheelSlipping = MutableStateFlow(false)

    // Optional: is the path clear? Derived inverse
    val pathIsClear: StateFlow<Boolean> = derivedStateFlow(obstacleDetected) { !it }
}
