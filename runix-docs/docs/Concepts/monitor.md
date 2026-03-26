---
id: monitor
title: Monitors
sidebar_position: 1
---

# Monitors

A monitor watches state and evaluates a condition. When the condition is met, it emits a signal. Monitors observe and report. Deciding what to do in response is a reaction's job.

## The DSL

```kotlin
val batteryLow = monitor("BatteryLow") {
    batteryLevel.hasBeenBelowFor(20.0, forDuration = 10.seconds)
} emits batteryLowSignal
```

`monitor()` takes a name and a condition block. The condition block returns a `MonitoredCondition` built using the condition DSL on any `StateFlow`. The `emits` keyword is an infix function that connects the monitor to the signal it fires when the condition is met.

## How evaluation works

Monitors evaluate reactively, not by polling. When a `StateFlow` used in the condition emits a new value, the monitor re-evaluates. Between state changes, nothing runs.

If a condition requires time to accumulate, the monitor schedules its own re-evaluation at the earliest moment the condition could become true. If state changes before that recheck fires, the scheduled recheck is canceled and a fresh evaluation runs from the new state. Evaluations are never stale.

For the full evaluation model, including the three possible outcomes (True, False, Delayed) and how the memory system works, see [Monitored Conditions](./monitored-conditions/index.md).

## Condition types

Conditions are extension functions on `StateFlow`. There are two flavors.

**Snapshot conditions** check the current value with no memory:

```kotlin
val doorOpen = monitor("DoorOpen") {
    doorSensor.isTrue()
} emits doorOpenedSignal

val overheating = monitor("Overheating") {
    temperature.isAbove(90.0)
} emits overheatSignal
```

**Temporal conditions** reason over time and require history:

```kotlin
val batteryLow = monitor("BatteryLow") {
    batteryLevel.hasBeenBelowFor(20.0, forDuration = 10.seconds)
} emits batteryLowSignal

val motorUnstable = monitor("MotorUnstable") {
    motorRpm.hasFluctuatedBeyond(margin = 5.0, inLast = 3.seconds)
} emits motorUnstableSignal
```

Temporal conditions are the reason monitors exist as a concept. A plain reactive `if` can express snapshots. Temporal conditions require the memory-tracking and scheduling infrastructure that monitors provide.

Conditions also compose with `allOf`, `anyOf`, and `not`. See [Composing conditions](./monitored-conditions/index.md#composing-conditions) for details.

## Labeling flows for traceability

You can attach a label to a flow for clearer trace output and diagnostics:

```kotlin
val batteryLevel = rawBatteryFlow.label("battery.level")
```

`LabeledFlow` works with all the same condition extensions as `StateFlow`. The label appears in trace events and logs, making it easier to identify which input triggered a re-evaluation.

---

→ [Monitored Conditions](./monitored-conditions/index.md): the full reference for all condition types

→ [Signals](./signal.md): what happens when a monitor fires
