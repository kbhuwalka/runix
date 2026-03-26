---
id: cognitive-loop
title: The Cognitive Loop
sidebar_position: 3
slug: /cognitive-loop
---

# The Cognitive Loop

Runix introduces four primitives that sit above your existing state: Monitor, Signal, Reaction, and Action. This page explains how they connect and where data flows between them.

## The runtime

The Runix runtime has two external surfaces and one internal loop:

```
External inputs
      │
      ├── State changes  ──►  Monitor  ──►  Signal ──┐
      │                                              │
      └── Direct signal emission ───────────────────►│
                                                     ▼
                                                 Reaction
                                                     │
                                                     ▼
                                                  Action
                                                     │
                                              State changes ──► (back to Monitor)
```

State and Signals are both entry points. Anything external enters the runtime by updating state or emitting a signal directly. From there, monitors and reactions route behavior through the system. Actions can feed back into state, closing the loop.

## How data flows

**State** is where external input enters. Sensor readings, UI events, and network messages all arrive as updates to a `MutableStateFlow`. When state changes, monitors that observe it re-evaluate.

**Monitors** sit between state and signals. They evaluate conditions, including conditions that reason over time, and emit a signal when met. A monitor never decides what to do; it only reports that something happened.

**Signals** are the decoupling point. They carry a typed event from the producer (a monitor, or any code that calls `emit()`) to the consumers (reactions). Neither side knows about the other. This is what makes pieces independently testable and replaceable.

**Reactions** subscribe to signals and respond. They are free-form suspend blocks that can update state, run actions, emit other signals, or call external systems. A reaction is the decision layer: given that something was detected, what should happen?

**Actions** are for work with operational requirements. If a task is long-running, shouldn't overlap with itself, needs a timeout, or needs a structured result, it belongs in an action. Actions return `ActionResult`: `Success`, `Failure`, `AlreadyRunning`, `Cancelled`, or `Timeout`.

When an action or reaction updates state, any monitor watching that state re-evaluates, and the loop continues.

## Registration

Every primitive must be registered in an `AppModule` to participate in the runtime. Primitives that aren't registered are never started.

```kotlin
object SafetyModule : AppModule("Safety") {
    init {
        defineBehavior {
            +tempMonitor
            +onUnsafeTemperature
            +moveToPosition
        }
    }
}
```

## What's next

- [Monitors](./Concepts/monitor.md): condition types and evaluation in depth
- [Signals](./Concepts/signal.md): typed events and decoupling
- [Reactions](./Concepts/reaction.md): the decision layer
- [Actions](./Concepts/action.md): managed processes
- [Monitored Conditions](./Concepts/monitored-conditions/index.md): the full set of condition types