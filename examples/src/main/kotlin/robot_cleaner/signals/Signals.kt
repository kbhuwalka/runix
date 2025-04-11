package robot_cleaner.signals

import runix.primitives.Signal

object Signals {
    // System-level
    val startCleaning = object : Signal("startCleaning") {}
    val pauseCleaning = object : Signal("pauseCleaning") {}
    val resumeCleaning = object : Signal("resumeCleaning") {}
    val stopCleaning = object : Signal("stopCleaning") {}
    val cleaningFailed = object : Signal("cleaningFailed") {}

    // Dock / power
    val returnToBase = object : Signal("returnToBase") {}
    val startCharging = object : Signal("startCharging") {}
    val emptyBin = object : Signal("emptyBin") {}

    // UI / experience
    val dimLights = object : Signal("dimLights") {}
    val playChime = object : Signal("playChime") {}

    // Safety / user
    val userStop = object : Signal("userStop") {}
    val userPresent = object : Signal("userPresent") {}

    // Safety / sensor issues
    val wheelStuck = object : Signal("wheelStuck") {}
    val wheelSlipping = object : Signal("wheelSlipping") {}
}
