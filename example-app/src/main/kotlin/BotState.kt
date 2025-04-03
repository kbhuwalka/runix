import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

// -------------- BotState --------------
object BotState {
    val isCalibrating = MutableStateFlow(false)
    val isMoving = MutableStateFlow( false)
    val currentSpeed = MutableStateFlow( 0.0)
    val batteryLevel = MutableStateFlow(100)
    val isCharging = MutableStateFlow( false)
    val deliveriesCompleted = MutableStateFlow( 0)
    val temperature = MutableStateFlow( 60.0)
    val currentLocation = MutableStateFlow("dock")

    val isLowSpeedDuringCalibration = isCalibrating
        .combine(currentSpeed) { calibrating, speed ->
            calibrating && speed < 5
        }
        .stateIn(CoroutineScope(Dispatchers.Default), SharingStarted.Eagerly, false)
}
