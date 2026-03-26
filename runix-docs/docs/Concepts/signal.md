---
id: signal
title: Signals
sidebar_position: 3
---

# Signals

A signal is a typed event that travels through the runtime. Monitors emit signals when their conditions are met. Reactions subscribe to signals and run when they arrive. Anything in the system can also emit a signal directly.

Signals are the decoupling layer between detection and response. A monitor doesn't know what will happen when it fires. A reaction doesn't know what caused the signal. That separation makes both independently testable and changeable.

## Declaring a signal

```kotlin
fun <T> signal(name: String): SignalHandle<T>
```

Signals are created with the `signal()` function and a type parameter:

```kotlin
val unsafeTemperature = signal<Unit>("UnsafeTemperature")
val speedUpdate = signal<Double>("SpeedUpdate")
val errorReport = signal<ErrorInfo>("ErrorReport")
```

The type parameter `T` defines what payload the signal carries. Use `Unit` for signals that are pure events with no data. Use a specific type when the receiver needs context about what happened.

## Emitting a signal

```kotlin
suspend fun SignalHandle<T>.emit(value: T)
```

Signals are emitted with `emit()`, which is a suspending function:

```kotlin
unsafeTemperature.emit(Unit)
speedUpdate.emit(2.5)
errorReport.emit(ErrorInfo(code = "E001", message = "Sensor offline"))
```

`emit()` can be called from anywhere: a reaction handler, an action body, external code, a coroutine you launched yourself. The signal bus delivers the emission to every reaction subscribed to that signal.

## Signals and monitors

Monitors connect to signals through the `emits` keyword:

```kotlin
val tempMonitor = monitor("UnsafeTemperatureSustained") {
    temperature.hasBeenAboveFor(40.0, forDuration = 15.minutes)
} emits unsafeTemperature
```

When the monitor's condition is met, it calls `emit(Unit)` on the signal internally. This is why monitor signals are always `SignalHandle<Unit>`, since the monitor has no payload to attach beyond the fact that the condition was met.

For signals that carry data, emit them directly from a reaction or action instead of through a monitor.

## Multiple subscribers

Multiple reactions can subscribe to the same signal. They run independently and concurrently when the signal is emitted:

```kotlin
val logTemperature = reaction(
    name = "LogUnsafeTemperature",
    signal = unsafeTemperature
) {
    logger.warn("Unsafe temperature detected")
}

val activateCooling = reaction(
    name = "ActivateCooling",
    signal = unsafeTemperature
) {
    FoodSafetyState.coolingActive.value = true
}
```

Both reactions fire when `unsafeTemperature` is emitted. Neither knows about the other.

---

→ [Reactions](./reaction.md): what happens when a signal arrives