package robot_cleaner.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import runix.dsl.derivedSignal

object CleaningState {
    val isCleaning = MutableStateFlow(false)
    val binCapacity = MutableStateFlow(0) // percentage
    val isBinFull = derivedSignal(binCapacity) { it >= 90 }
    val cleaningStopped = MutableStateFlow(false)

    // Derived: indicates the robot is idle (not cleaning + not charging)
    val isIdle: StateFlow<Boolean> = derivedSignal(isCleaning) { !it }
    val cleaningFailed = MutableStateFlow(false)
}
