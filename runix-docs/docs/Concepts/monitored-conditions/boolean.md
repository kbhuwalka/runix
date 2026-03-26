---
id: boolean-conditions
title: Boolean Conditions
sidebar_position: 1
---

# Boolean Conditions

Boolean conditions work on `StateFlow<Boolean>` and `LabeledFlow<Boolean>` values. Use them for flags, switches, binary sensors, or any value that is either true or false.

## Snapshot

### isTrue

```kotlin
fun StateFlow<Boolean>.isTrue(): MonitoredCondition
```

True when the current value is `true`. No memory, no history.

```kotlin
doorSensor.isTrue()
```

Use this when you only care about the current state: a door is open, a motor is running, a flag is set.

## Persistence

### hasBeenTrueFor

```kotlin
fun StateFlow<Boolean>.hasBeenTrueFor(
    forDuration: Duration
): MonitoredCondition
```

True only if the value has been `true` continuously for the full duration. If the value drops to `false` at any point, the clock resets.

```kotlin
alarmActive.hasBeenTrueFor(5.seconds)
```

Use this to filter out transient spikes. A sensor that flickers to `true` for a fraction of a second and back won't satisfy a persistence check, but one that stays true for the full duration will.

## Historical

### wasTrueFor

```kotlin
fun StateFlow<Boolean>.wasTrueFor(
    forDuration: Duration,
    inLast: Duration
): MonitoredCondition
```

True if the value was `true` for at least `forDuration` continuously, at some point within the `inLast` window. The value does not need to still be `true` now.

```kotlin
motionDetected.wasTrueFor(2.seconds, inLast = 30.seconds)
```

Use this when you need to know that something happened recently, not that it's still happening. "Was the warning light on for at least 10 seconds in the last hour?" is a historical question.

### wasEverTrue

```kotlin
fun StateFlow<Boolean>.wasEverTrue(
    inLast: Duration
): MonitoredCondition
```

True if the value was `true` at any point within the `inLast` window, even briefly.

```kotlin
doorOpened.wasEverTrue(inLast = 10.seconds)
```

Use this to catch transient events. Unlike `wasTrueFor`, there's no minimum duration. A single momentary flicker to `true` is enough.

## Fluctuation

### hasFluctuated

```kotlin
fun StateFlow<Boolean>.hasFluctuated(
    inLast: Duration
): MonitoredCondition
```

True if the value changed at least once within the `inLast` window. The direction of change doesn't matter. A flip from `true` to `false` or `false` to `true` both count.

```kotlin
connectionStatus.hasFluctuated(inLast = 5.seconds)
```

Use this to detect instability. A connection that keeps toggling, a sensor that bounces between states, or a flag that can't settle. Often combined with `not()` to express stability: `not(connectionStatus.hasFluctuated(inLast = 10.seconds))` means the connection has been stable for the last 10 seconds.

---

→ [Numeric Conditions](./numeric.md): conditions for measurable values