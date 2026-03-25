---
id: introduction
title: Introduction
slug: /
---

# What is Runix

Runix is a reactive cognitive framework for real-time systems.  
It transforms how you write decision logic—introducing a model where perception, reasoning, and action are time-aware, signal-driven, and fully traceable.

### The Problem

Most control logic in robotics and real-time systems still relies on:

- Polling loops that constantly check state
- Timers that must be manually managed
- State machines that grow brittle as they grow
- Conditionals without memory or context

These approaches are difficult to scale, debug, or reason about. They conflate *how* the system thinks with *what* it should think about.

## A Better Foundation

Runix begins with a shift in how we frame conditions:

> Logic isn’t just about what is true—it’s about **what has been true**, **how long it has been true**, and **what that means in context**.

That idea—small on the surface—leads to a new architecture.

Runix introduces a cognitive execution loop that gives your system:

- Time-awareness
- Memory of past values and events
- Reactive signal emission
- Declarative condition structure
- Precise, traceable actions

## From Logic to Cognition

Runix encourages you to write logic that behaves more like cognition:

- “Has this condition persisted for more than 5 seconds?”
- “Did this failure happen multiple times recently?”
- “Has the system been quiet for a while?”

These are not edge cases.  
They're how real systems behave under real load.

For example, consider a food storage robot that must comply with food safety guidelines, such as keeping items below 40°F (4.4°C). You might express this logic as:

```kotlin
monitor("UnsafeTemperatureSustained") {
    dependsOn(temperatureSensor)

    condition {
        temperatureSensor.map { it > 40.0 }
            .persistedFor(15.minutes, key = "TempTooHigh")
            .invoke()
    }

    trigger(Signal.UnsafeTemperatureDetected)
}
```

This monitor will emit a signal only if the temperature has exceeded 40°F for 15 continuous minutes.  
There are no timers. No manual resets. The condition holds memory—and when it's met, it acts.

Runix encourages you to write logic that behaves more like cognition:

- “Has this condition persisted for more than 5 seconds?”
- “Did this failure happen multiple times recently?”
- “Has the system been quiet for a while?”

These are not edge cases.  
They're how real systems behave under real load.

## What You Get

- **Declarative monitors** that track conditions over time
- **Signal-driven reactions** that execute logic in response
- **Temporal expressions** like `.persistedFor`, `.wasSilentFor`, and `.occurredAtLeast`
- **Precise re-evaluation scheduling** with no polling
- **A reactive runtime** that scales with your system—not against it

---

## Where to Go Next

- [Get Started](./getting-started.md)
- [Learn about Monitors](./concepts/monitor.md)
- [Explore Temporal Expressions](./concepts/temporal.md)
- [Browse the DSL Reference](./dsl/reference.md)

Runix gives you a way to think clearly about system behavior over time—without rewriting the same logic, or managing the same timers, again and again.
