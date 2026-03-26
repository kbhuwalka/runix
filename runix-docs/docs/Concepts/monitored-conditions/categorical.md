---
id: categorical-conditions
title: Categorical Conditions
sidebar_position: 3
---

# Categorical Conditions

Categorical conditions work on `StateFlow<T : Enum<T>>` and `LabeledFlow<T : Enum<T>>` values. Use them for robot modes, system states, lifecycle stages, or any value drawn from a fixed set of options.

Beyond snapshot and persistence checks, categorical conditions can reason about transitions, exits, and ordered sequences of state changes.

## Snapshot

### isIn

```kotlin
fun <T : Enum<T>> StateFlow<T>.isIn(
    expected: T
): MonitoredCondition
```

True when the current value equals the expected state.

```kotlin
robotMode.isIn(RobotMode.IDLE)
```

Use this when you only care about the current state, not how the system got there or how long it has been there.

## Persistence

### hasBeenIn

```kotlin
fun <T : Enum<T>> StateFlow<T>.hasBeenIn(
    expected: T,
    forDuration: Duration
): MonitoredCondition
```

True only if the value has been equal to the expected state continuously for the full duration.

```kotlin
robotMode.hasBeenIn(RobotMode.IDLE, forDuration = 2.seconds)
```

Use this to filter out transient state visits. A brief pass through IDLE on the way to another state won't satisfy a 2-second persistence check.

## Historical

### wasIn

```kotlin
fun <T : Enum<T>> StateFlow<T>.wasIn(
    expected: T,
    forDuration: Duration,
    inLast: Duration
): MonitoredCondition
```

True if the value was equal to the expected state for at least `forDuration` continuously, at some point within the `inLast` window. The value does not need to be in that state now.

```kotlin
robotMode.wasIn(RobotMode.CHARGING, forDuration = 5.minutes, inLast = 1.hours)
```

### wasEverIn

```kotlin
fun <T : Enum<T>> StateFlow<T>.wasEverIn(
    expected: T,
    inLast: Duration
): MonitoredCondition
```

True if the value was equal to the expected state at any point within the `inLast` window, even briefly.

```kotlin
robotMode.wasEverIn(RobotMode.ERROR, inLast = 10.minutes)
```

Use historical conditions to reason about the past. "Was the robot ever in an error state in the last 10 minutes?" is a question about history, not the present.

## Transitions

Transition conditions reason about how the system arrived at its current state, not just where it is now. They are unique to the categorical family.

### transitionedTo

```kotlin
fun <T : Enum<T>> StateFlow<T>.transitionedTo(
    expected: T,
    forDuration: Duration = Duration.ZERO,
    inLast: Duration
): MonitoredCondition
```

True if the value entered the expected state and remained there for at least `forDuration`, within the `inLast` window. `forDuration` defaults to zero, meaning any entry counts.

```kotlin
robotMode.transitionedTo(RobotMode.IDLE, forDuration = 1.seconds, inLast = 30.seconds)
```

Use this to detect entry into a state. "The robot entered IDLE and held it for at least 1 second" is different from "the robot is currently in IDLE." The first implies a transition happened; the second says nothing about how it got there.

### transitionedFrom

```kotlin
fun <T : Enum<T>> StateFlow<T>.transitionedFrom(
    expected: T,
    forDuration: Duration = Duration.ZERO,
    inLast: Duration
): MonitoredCondition
```

True if the value left the expected state and stayed out for at least `forDuration`, within the `inLast` window. `forDuration` defaults to zero.

```kotlin
robotMode.transitionedFrom(RobotMode.ERROR, forDuration = 2.seconds, inLast = 30.seconds)
```

Use this to detect recovery. "The robot left the ERROR state and stayed out for at least 2 seconds" means the error wasn't just a momentary glitch that came back.

### transitionedThrough

```kotlin
fun <T : Enum<T>> StateFlow<T>.transitionedThrough(
    vararg states: T,
    inLast: Duration
): MonitoredCondition
```

True if the value passed through the given states in exact order, at any point within the `inLast` window.

```kotlin
robotMode.transitionedThrough(
    RobotMode.ACTIVE,
    RobotMode.ERROR,
    RobotMode.IDLE,
    inLast = 30.seconds
)
```

Use this to verify a specific path through the state space. "The robot was active, entered an error, then recovered to idle" is a recovery sequence. The order matters. Intermediate states between the specified ones are ignored, so the system only checks that these states appeared in this sequence within the window.

This is particularly useful for safety and recovery logic where it's not enough to know the current state. You need to know how the system got there.

---

→ [Signals](../signal.md): how conditions connect to the rest of the system