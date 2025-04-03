import kotlinx.coroutines.flow.MutableStateFlow
import runix.primitives.MutableRunixFlow
import runix.primitives.mutableRunixFlow

// -------------- BotState --------------
object BotState {
    val isCalibrating = mutableRunixFlow("isCalibrating", false)
    val isMoving = mutableRunixFlow("isMoving", false)
    val currentSpeed = mutableRunixFlow("currentSpeed", 0.0)
    val batteryLevel = mutableRunixFlow("batteryLevel", 100)
    val isCharging = mutableRunixFlow("isCharging", false)
    val deliveriesCompleted = mutableRunixFlow("deliveriesCompleted", 0)
    val temperature = mutableRunixFlow("temperature", 60.0)
    val currentLocation = mutableRunixFlow("currentLocation", "dock")
}
