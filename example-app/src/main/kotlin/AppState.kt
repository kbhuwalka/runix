import runix.primitives.MutableRunixFlow
import runix.primitives.runixMutable

object BotState {
    val atDropZone = runixMutable("atDropZone", false)
    val dropSuccessful = runixMutable("dropSuccessful", true)
    val batteryLevel = runixMutable("batteryLevel", 100)
    val isCharging = runixMutable("isCharging", false)
    val currentZone = runixMutable("currentZone", "home")
    val dropAttempts = runixMutable("dropAttempts", 0)
}