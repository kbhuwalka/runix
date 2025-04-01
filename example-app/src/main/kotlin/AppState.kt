import kotlinx.coroutines.flow.MutableStateFlow
import runix.primitives.runixMutable

object BotState {
    val batteryLevel = runixMutable("batteryLevel", 100)
    val isCleaning = runixMutable("isCleaning", false)
    val location = runixMutable("location", "dock")
    val obstacleDetected = runixMutable("obstacleDetected", false)
    val startRequested = runixMutable("startRequested", false)
    val pathClear = runixMutable("pathClear", true)
    val waterTankFull = runixMutable("waterTankFull", true)
}