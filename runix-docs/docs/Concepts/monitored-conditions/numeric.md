---
id: numeric-conditions
title: Numeric Conditions
sidebar_position: 2
---

# Numeric Conditions

Numeric conditions work on `StateFlow<Double>` and `LabeledFlow<Double>` values. Use them for sensor readings, voltages, temperatures, speeds, or any continuously measurable value.

## Snapshot

### isAbove

```kotlin
fun StateFlow<Double>.isAbove(
    threshold: Double
): MonitoredCondition
```

True when the current value is strictly greater than the threshold.

```kotlin
temperature.isAbove(80.0)
```

### isBelow

```kotlin
fun StateFlow<Double>.isBelow(
    threshold: Double
): MonitoredCondition
```

True when the current value is strictly less than the threshold.

```kotlin
batteryLevel.isBelow(15.0)
```

Use snapshot conditions when you only care about the current reading, not its history.

## Persistence

### hasBeenAboveFor

```kotlin
fun StateFlow<Double>.hasBeenAboveFor(
    threshold: Double,
    forDuration: Duration
): MonitoredCondition
```

True only if the value has remained strictly above the threshold continuously for the full duration.

```kotlin
temperature.hasBeenAboveFor(80.0, forDuration = 30.seconds)
```

### hasBeenBelowFor

```kotlin
fun StateFlow<Double>.hasBeenBelowFor(
    threshold: Double,
    forDuration: Duration
): MonitoredCondition
```

True only if the value has remained strictly below the threshold continuously for the full duration.

```kotlin
pressure.hasBeenBelowFor(20.0, forDuration = 10.seconds)
```

Use persistence conditions to guard against transient spikes or dips. A brief temperature spike above 80 won't satisfy a 30-second persistence check, but a sustained reading will.

## Historical

### wasAbove

```kotlin
fun StateFlow<Double>.wasAbove(
    threshold: Double,
    forDuration: Duration,
    inLast: Duration
): MonitoredCondition
```

True if the value was above the threshold for at least `forDuration` continuously, at some point within the `inLast` window. The value does not need to still be above the threshold now.

```kotlin
temperature.wasAbove(85.0, forDuration = 30.seconds, inLast = 5.minutes)
```

### wasBelow

```kotlin
fun StateFlow<Double>.wasBelow(
    threshold: Double,
    forDuration: Duration,
    inLast: Duration
): MonitoredCondition
```

True if the value was below the threshold for at least `forDuration` continuously, at some point within the `inLast` window.

```kotlin
cpuUsage.wasBelow(10.0, forDuration = 5.seconds, inLast = 1.minutes)
```

### wasEverAbove

```kotlin
fun StateFlow<Double>.wasEverAbove(
    threshold: Double,
    inLast: Duration
): MonitoredCondition
```

True if the value exceeded the threshold at any point within the `inLast` window, even briefly.

```kotlin
temperature.wasEverAbove(100.0, inLast = 2.minutes)
```

### wasEverBelow

```kotlin
fun StateFlow<Double>.wasEverBelow(
    threshold: Double,
    inLast: Duration
): MonitoredCondition
```

True if the value dropped below the threshold at any point within the `inLast` window, even briefly.

```kotlin
voltage.wasEverBelow(3.0, inLast = 30.seconds)
```

Use historical conditions to reason about what happened recently, not just what's happening now. `wasEverAbove` catches brief spikes that may have already passed. `wasAbove` with a `forDuration` ensures the spike was sustained.

## Trend

### isIncreasing

```kotlin
fun StateFlow<Double>.isIncreasing(
    forDuration: Duration
): MonitoredCondition
```

True if the value has been monotonically increasing for at least `forDuration`.

```kotlin
fanSpeed.isIncreasing(forDuration = 15.seconds)
```

### isDecreasing

```kotlin
fun StateFlow<Double>.isDecreasing(
    forDuration: Duration
): MonitoredCondition
```

True if the value has been monotonically decreasing for at least `forDuration`.

```kotlin
temperature.isDecreasing(forDuration = 10.seconds)
```

Use trend conditions to detect ramp-ups, cool-downs, or other directional movement. Any reversal, however brief, resets the duration counter.

## Stability

### isStableWithin

```kotlin
fun StateFlow<Double>.isStableWithin(
    margin: Double,
    inLast: Duration
): MonitoredCondition
```

True if the spread between the minimum and maximum values has stayed within the margin for the entire `inLast` window.

```kotlin
rpm.isStableWithin(margin = 5.0, inLast = 20.seconds)
```

Use this to confirm a value has settled. A motor that holds between 995 and 1005 RPM over 20 seconds is stable within a margin of 10.

### hasFluctuatedBeyond

```kotlin
fun StateFlow<Double>.hasFluctuatedBeyond(
    margin: Double,
    inLast: Duration
): MonitoredCondition
```

True if the spread between the minimum and maximum values exceeded the margin at any point within the `inLast` window.

```kotlin
temperature.hasFluctuatedBeyond(margin = 3.0, inLast = 1.minutes)
```

Use this to detect volatility. Often combined with `not()` as an alternative way to express stability: `not(temperature.hasFluctuatedBeyond(...))` means the temperature has remained stable.

---

→ [Categorical Conditions](./categorical.md): conditions for enum states and transitions