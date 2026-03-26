---
id: monitored-conditions
title: Monitored Conditions
---

# Monitored Conditions

A `MonitoredCondition` is what you place in a monitor's condition block. It describes what must be true for the monitor to fire, and unlike a plain boolean, it can reason over time, carry memory, and tell the runtime when to check again.

## Why not just a boolean

A plain boolean expression answers a single question: is this true right now?

```kotlin
// plain boolean, only the current value
temperature.value > 80.0
```

But real system conditions rarely care about a single moment. They ask:

- Has this been true for long enough to act on?
- Did this happen recently, even if it's no longer happening?
- Did the system follow the right path to get here?

These questions require memory, a record of what values were observed and when. A `MonitoredCondition` provides that automatically.

```kotlin
// MonitoredCondition, knows about history
temperature.hasBeenAboveFor(80.0, forDuration = 30.seconds)
```

## Three-outcome evaluation

When a `MonitoredCondition` is evaluated, the result is one of three things:

- **True**: the condition is met, the monitor emits its signal
- **False**: the condition is not met, the monitor waits for the next state change
- **Delayed**: the condition cannot be determined yet, but it knows the earliest time it could become true

Snapshot conditions like `isTrue()`, `isAbove()`, and `isIn()` only ever return True or False. Delayed is exclusive to temporal conditions, where time must accumulate before an answer is possible.

Delayed is what makes time-based conditions work without polling. When `hasBeenAboveFor(80.0, forDuration = 30.seconds)` is evaluated 10 seconds into the threshold crossing, it knows the condition can't be true yet, but it can calculate exactly when to check again. The monitor schedules that recheck automatically.

If state changes before the recheck fires, the scheduled recheck is canceled and a fresh evaluation runs from the new state. Evaluations are never stale.

## The memory model

You never interact with memory tracking directly. It's created, updated, and queried automatically as part of condition evaluation.

Every condition that needs history maintains a tracker for the flow it observes. As the flow emits values, the tracker records each value with a timestamp. When the `MonitoredCondition` is evaluated, it queries that history to answer its question: has the value been above the threshold continuously? Did it follow this sequence of states?

History is bounded. The `inLast` window you pass to a condition determines how far back is retained. History older than the longest active window is pruned automatically.

Memory usage depends on two things: the retention window and the frequency of updates. A flow that emits once per second over a 5-minute window retains at most 300 entries. A noisy sensor emitting hundreds of times per second over the same window retains far more. For high-frequency flows, keep retention windows as short as your logic requires.

## The three families

Conditions are organized by the type of value they observe. Each family has its own tracker appropriate to what it measures.

**Boolean conditions** work on `StateFlow<Boolean>` values: flags, sensors, any true/false signal.

```kotlin
doorSensor.isTrue()
alarmActive.hasBeenTrueFor(forDuration = 5.seconds)
motionDetected.wasEverTrue(inLast = 30.seconds)
```

→ [Boolean Conditions](./boolean.md)

**Numeric conditions** work on `StateFlow<Double>` values: temperatures, voltages, speeds, any measurable value.

```kotlin
temperature.isAbove(80.0)
battery.hasBeenBelowFor(15.0, forDuration = 10.seconds)
rpm.isStableWithin(margin = 5.0, inLast = 20.seconds)
```

→ [Numeric Conditions](./numeric.md)

**Categorical conditions** work on `StateFlow<T : Enum<T>>` values: robot modes, system states, lifecycle stages.

```kotlin
robotMode.isIn(RobotMode.IDLE)
robotMode.hasBeenIn(RobotMode.IDLE, forDuration = 2.seconds)
robotMode.transitionedThrough(ACTIVE, ERROR, IDLE, inLast = 30.seconds)
```

→ [Categorical Conditions](./categorical.md)

## Composing conditions

Conditions compose with `allOf`, `anyOf`, and `not`. When you compose conditions, you're building a tree with individual conditions at the leaves and combinators as the branches:

```kotlin
allOf(                                                              // branch
    robotMode.hasBeenIn(RobotMode.IDLE, forDuration = 2.seconds),  // leaf
    battery.isAbove(15.0),                                         // leaf
    not(                                                           // branch
        rpm.hasFluctuatedBeyond(margin = 5.0, inLast = 3.seconds) // leaf
    )
)
```

The runtime walks this tree at activation time to discover which flows to observe, so you never need to declare dependencies explicitly. The tree also defines how evaluation results compose: `allOf` requires all children to be true, `anyOf` requires at least one. Delayed propagates through the tree correctly. If any condition returns Delayed, the runtime schedules the soonest recheck across all children.

---

→ [Signals](../signal.md): how conditions connect to the rest of the system
