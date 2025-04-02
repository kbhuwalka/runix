import runix.primitives.mutableRunixFlow

object BotState {
    val atDropZone = mutableRunixFlow("atDropZone", false)
    val dropSuccessful = mutableRunixFlow("dropSuccessful", true)
    val batteryLevel = mutableRunixFlow("batteryLevel", 100)
    val isCharging = mutableRunixFlow("isCharging", false)
    val currentZone = mutableRunixFlow("currentZone", "home")
    val dropAttempts = mutableRunixFlow("dropAttempts", 0)
}