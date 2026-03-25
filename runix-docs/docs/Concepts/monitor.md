---
id: monitor
title: Monitors
---

# Monitors

Monitors are the cognitive sensors of Runix. They continuously evaluate conditions over time, and emit signals when those conditions are met.

Unlike traditional polling loops or reactive `if` statements, monitors in Runix are declarative, reactive, and memory-aware. They only evaluate when their input state changes—and when they do, they consider not just *what* is true, but *for how long* it has been true.

---

## A Simple Monitor

Here's a basic example of a monitor that watches a battery level and emits a signal if it stays below 20% for 10 seconds.

```kotlin
monitor("BatteryLow") {
    dependsOn(batteryLevel)

    condition {
        batteryLevel.map { it < 20 }
            .persistedFor(10.seconds, "BatteryLowCondition")
            .invoke()
    }

    trigger(Signal.BatteryLow)
}
```

This monitor will emit the `BatteryLow` signal only if the battery level remains under 20 continuously for 10 seconds. If the condition is interrupted (e.g. the battery level rises above 20), the timer resets.

---

## Defining Input State

Monitors observe `StateFlow<T>` inputs. These are typically defined using:

```kotlin
val batteryLevel = MutableStateFlow(100)
```

You can depend on as many flows as needed:

```kotlin
dependsOn(batteryLevel, isCharging, temperature)
```

Monitors will only re-evaluate when one of the dependencies emits a new value.

---

## Condition Evaluation

The `condition` block returns either a `Boolean` or a `ConditionEval` (via `.persistedFor`, `.wasSilentFor`, etc). You don't need to think about these types—just focus on writing your logic declaratively.

You can use temporal expressions like:

```kotlin
.map { it < 30 }.persistedFor(5.seconds, key = "LowTemp").invoke()
```

Or simple state:

```kotlin
condition { doorIsOpen.value && !motorRunning.value }
```

---

## Triggering Signals

When a monitor's condition is met, it emits a signal using the `trigger(...)` block:

```kotlin
trigger(Signal.MyConditionMet)
```

That signal is then received by any reactions that are subscribed to it.

---

## Optional Throttling

Monitors can include a `throttle(...)` to prevent the same condition from triggering too frequently.

```kotlin
throttle(30.seconds)
```

This ensures that once a monitor fires, it won’t fire again until the throttle window has passed—even if the condition remains true.

---

## Monitor Behavior Summary

| Feature               | Behavior                                          |
|-----------------------|---------------------------------------------------|
| **Reactive**          | Evaluates only when state changes                 |
| **Time-aware**        | Can track persistence, silence, and event count   |
| **Signal-driven**     | Emits named signals, decoupled from direct logic  |
| **Declarative**       | Focus on what should happen, not how              |
| **Fully observable**  | Every monitor evaluation is traceable             |

---

Monitors form the foundation of Runix’s cognitive loop. They give your system memory, structure, and time-aware behavior—without the boilerplate of timers, loops, or state machines.
