---
id: temporal
title: Temporal Expressions
---

# Temporal Expressions

Temporal expressions give Runix its cognitive memory. They let the system evaluate **not just what is true**, but **for how long**, **how often**, or **how recently** something has been true.

This allows your monitors to behave like thought processes—not just conditions.

---

## `.persistedFor(...)`

Tracks if a condition has remained continuously true for a defined duration.

```kotlin
condition {
    temperature.map { it > 85 }
        .persistedFor(3.seconds, "OverheatCheck")
        .invoke()
}
```

- Only returns true if the condition stays true without interruption
- If the value changes during the window, the timer resets
- Under the hood, this is scheduled reactively—no polling required

---

## `.wasSilentFor(...)`

Checks if a condition has **not been true** for a while. Useful for detecting absence or inactivity.

```kotlin
condition {
    doorOpen.map { it }
        .wasSilentFor(30.seconds, "DoorClosedRecently")
        .invoke()
}
```

This is how Runix can model silence as a first-class signal.

---

## `.occurredAtLeast(...)`

Tests if a condition has occurred **N or more times** in a time window.

```kotlin
condition {
    motorFailures
        .occurredAtLeast(3, inLast = 1.minutes, key = "FailureCount")
        .invoke()
}
```

Useful for debouncing, retry thresholds, or escalation logic.

---

## `.lastOccurredWithin(...)`

Checks if something was true **recently**.

```kotlin
condition {
    pressureDrop
        .lastOccurredWithin(2.minutes, key = "RecentDrop")
        .invoke()
}
```

Ideal for cooldown periods or time-sensitive reactions.

---

## Numeric Expressions

You can also reason about `Flow<Double>` values:

```kotlin
condition {
    temperature.averageOver(5.minutes, threshold = 80.0, key = "AvgTemp").invoke()
}
```

Available methods include:

- `.averageOver(...)`
- `.varianceOver(...)`

These are designed for systems where a raw value is too noisy, but statistical reasoning is meaningful.

---

## Defining Temporal Expressions

All temporal expressions work on `Flow<Boolean>` or `Flow<Double>`, and require a stable `key`:

```kotlin
condition {
    batteryLevel.map { it < 20 }
        .persistedFor(10.seconds, key = "BatteryDrop")
        .invoke()
}
```

This `key` ensures that Runix tracks the state across monitor evaluations.

---

## Execution Model

Runix schedules reevaluations **only when needed**:

- If a monitor’s condition isn’t met but **may become valid later**, a recheck is scheduled
- If the condition becomes false in the meantime, the recheck is cancelled
- All rechecks are one-shot and precise—never polled

---

Temporal expressions turn conditions into cognition. They give your system memory, silence detection, and time-bound reasoning—without writing a single timer.
