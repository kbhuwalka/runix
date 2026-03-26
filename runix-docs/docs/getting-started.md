---
id: getting-started
title: Getting Started
sidebar_position: 2
slug: /getting-started
---

# Getting Started

This page walks through a complete Runix application from scratch. By the end you'll have seen the core primitives (state, signal, monitor, and reaction) and how they wire together into a running app.

## The scenario

A food storage system needs to comply with safety guidelines: if a bin's temperature rises above 40°F and stays there for 15 continuous minutes, the cooling system must activate and an alert must be sent.

## Step 1: Define state

State is a plain `MutableStateFlow`, standard Kotlin. There's nothing Runix-specific to register. Any flow can be observed by a monitor.

```kotlin
object FoodSafetyState {
    val temperature = MutableStateFlow(36.0)
    val coolingActive = MutableStateFlow(false)
}
```

## Step 2: Declare a signal

Signals decouple what is detected from what happens in response. The monitor will emit this signal; the reaction will receive it.

```kotlin
val unsafeTemperature = signal<Unit>("UnsafeTemperature")
```

## Step 3: Declare a monitor

A monitor watches state and evaluates a condition. When the condition is met, it emits the signal.

```kotlin
val temperatureMonitor = monitor("UnsafeTemperatureSustained") {
    FoodSafetyState.temperature.hasBeenAboveFor(40.0, forDuration = 15.minutes)
} emits unsafeTemperature
```

`hasBeenAboveFor` is a temporal condition. It tracks how long the value has continuously exceeded the threshold and re-evaluates only when `temperature` changes. No timers, no manual resets.

If the temperature drops below 40.0 at any point, the condition resets and the 15-minute clock starts over.

## Step 4: Declare a reaction

A reaction subscribes to a signal and runs a suspend block when it arrives. It can update state, emit other signals, or call external systems.

```kotlin
val onUnsafeTemperature = reaction(
    name = "HandleUnsafeTemperature",
    signal = unsafeTemperature
) {
    FoodSafetyState.coolingActive.value = true
    println("Alert: temperature unsafe. Food may be at risk.")
}
```

## Step 5: Create a module

Modules own the lifecycle of their primitives. Registration tells the runtime which monitors and reactions to start, and ensures they're stopped cleanly on shutdown. Declaring a primitive without registering it has no effect.

```kotlin
object FoodSafetyModule : AppModule("FoodSafety") {
    init {
        defineBehavior {
            +temperatureMonitor
            +onUnsafeTemperature
        }
    }
}
```

## Step 6: Create and start the app

An `App` installs modules and runs the runtime.

```kotlin
object FoodSafetyApp : App() {
    init {
        install(FoodSafetyModule)
    }
}

fun main() {
    FoodSafetyApp.start()
}
```

`start()` blocks the current thread and keeps the app running until the process is terminated. The runtime handles activation, shutdown hooks, and cleanup automatically.

## What happens at runtime

1. `temperatureMonitor` activates and begins watching `FoodSafetyState.temperature`
2. Each time `temperature` changes, the condition re-evaluates
3. Once `temperature` has been above 40.0 for 15 continuous minutes, the monitor emits `unsafeTemperature`
4. The runtime delivers the signal to `onUnsafeTemperature`
5. The reaction sets `coolingActive` to `true` and prints the alert

## Next steps

- [The Cognitive Loop](./cognitive-loop.md): how all the primitives connect
- [Actions](./Concepts/action.md): managed processes for long-running or concurrent work
- [Monitors](./Concepts/monitor.md): condition types and evaluation behavior
- [Monitored Conditions](./Concepts/monitored-conditions/index.md): the full set of condition types