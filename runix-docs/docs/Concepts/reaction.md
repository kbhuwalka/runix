---
id: reaction
title: Reactions
sidebar_position: 4
---

# Reactions

A reaction subscribes to a signal and runs a suspend block when it arrives. It's the decision layer of the system: given that something was detected, what should happen?

## Declaring a reaction

```kotlin
fun <T> reaction(
    name: String,
    signal: SignalHandle<T>,
    handler: suspend (T) -> Unit
): ReactionHandle<T>
```

A reaction takes a name, a signal to subscribe to, and a handler that receives the signal's payload:

```kotlin
val onUnsafeTemperature = reaction(
    name = "HandleUnsafeTemperature",
    signal = unsafeTemperature
) {
    FoodSafetyState.coolingActive.value = true
    println("Alert: temperature unsafe.")
}
```

For signals with a typed payload, the handler receives the value:

```kotlin
val onSpeedUpdate = reaction(
    name = "HandleSpeedUpdate",
    signal = speedUpdate
) { newSpeed ->
    motor.setTarget(newSpeed)
}
```

## What a reaction can do

The handler is a free-form coroutine. There are no restrictions on what it does:

- Update state directly: `FoodSafetyState.coolingActive.value = true`
- Run actions: `moveToPosition.run(targetCoords)`
- Emit other signals: `coolingActivated.emit(Unit)`
- Call external systems: `notificationService.send(...)`
- Launch coroutines for parallel work

Reactions are intentionally unconstrained. The constraint is on what they are, not what they do: a named, registered, observable response to a specific signal.

## Lifecycle

A reaction must be registered in a module to be active:

```kotlin
object SafetyModule : AppModule("Safety") {
    init {
        defineBehavior {
            +onUnsafeTemperature
        }
    }
}
```

When the module activates, the reaction subscribes to its signal on the signal bus. When the module deactivates, it unsubscribes. Reactions that aren't registered never receive signals.

## When to use a reaction vs. an action

Reactions and actions serve different purposes:

- A **reaction** is the immediate response: when this signal arrives, do this. It runs as a coroutine with no special execution semantics.
- An **action** is a managed process: it can be long-running, has concurrency control, can be queued, and returns a structured result.

If the work is simple and immediate (update a flag, log a message, emit another signal), do it directly in the reaction. If the work is long-running, shouldn't overlap with itself, or needs a timeout, call an action from the reaction.

---

→ [Actions](./action.md): managed processes for long-running or concurrent work