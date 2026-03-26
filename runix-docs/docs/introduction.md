---
id: introduction
title: Introduction
sidebar_position: 1
slug: /introduction
---

# What is Runix

Runix is a framework for writing decision logic in real-time systems. It gives your conditions **memory**, the ability to reason not just about what is true now, but what has been true, for how long, and in what sequence.

## The problem with traditional control logic

Most systems evaluate conditions as snapshots:

```kotlin
if (temperature > 40.0) {
    triggerAlert()
}
```

This works for simple cases. But real systems ask harder questions:

- Has the temperature been above 40°F for 15 continuous minutes?
- Did the error occur multiple times in the last hour?
- Has the system been in an idle state long enough to resume safely?

Answering these requires timers, flags, and history tracking, written by hand across every condition that needs it. The logic that matters gets buried in glue code.

## What Runix does differently

Runix makes time and memory first-class parts of how you write conditions. The same food safety check becomes:

```kotlin
val temperature = MutableStateFlow(36.0)
val unsafeTemperature = signal<Unit>("UnsafeTemperature")

val tempMonitor = monitor("UnsafeTemperatureSustained") {
    temperature.hasBeenAbove(40.0, forDuration = 15.minutes)
} emits unsafeTemperature
```

No timers. No manual resets. The condition tracks its own history and evaluates reactively, only when temperature changes.

When the condition is met, a signal is emitted. A reaction subscribed to that signal decides what to do.

## How behavior is structured

Your state stays as plain `MutableStateFlow` (standard Kotlin). Runix introduces four primitives that sit above it:

- **Monitors** watch state and evaluate conditions over time, emitting a signal when met
- **Signals** are typed events that decouple detection from response
- **Reactions** subscribe to signals and execute logic when they arrive
- **Actions** are managed processes with concurrency and queuing semantics

Each primitive has one job. Together they form a runtime where behavior is observable, testable, and easy to trace when something goes wrong.

## Where to go next

- [Getting Started](./getting-started.md): a full working example end to end
- [The Cognitive Loop](./cognitive-loop.md): how the primitives connect
- [Monitors](./Concepts/monitor.md): deep dives into each primitive, starting here