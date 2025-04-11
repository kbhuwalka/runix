package robot_cleaner.state

import kotlinx.coroutines.flow.MutableStateFlow

object UserState {
    val userPresent = MutableStateFlow(false)
    val userStopPressed = MutableStateFlow(false) // or: a SharedFlow<Unit> for push buttons
}
