---
id: getting-started
title: Getting Started
slug: /getting-started
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

# Getting Started

This guide introduces the fundamentals of Runix by walking through a complete, working example.

You'll see how each part of the system fits together—from defining state, to evaluating conditions, to triggering signals and executing reactions.


## A Full Cognitive Loop

This example models a food safety system. If the temperature of a storage bin remains above 40°F for 15 minutes, the system shuts down the cooling system and alerts the user.

<Tabs groupId="cognitive-loop">
  <TabItem value="state" label="State">

```kotlin
object States {
    val temperature = MutableStateFlow(36.0)
    val coolingActive = MutableStateFlow(false)
}
```

This defines the reactive state that the system observes. When `temperature` changes, monitors depending on it will re-evaluate.

  </TabItem>
  <TabItem value="signal" label="Signal">

```kotlin
object Signals {
    val UnsafeTemperature = Signal("UnsafeTemperature")
}
```

Signals are emitted when a monitor condition is met. They serve as the trigger point for reactions.

  </TabItem>
  <TabItem value="monitor" label="Monitor">

```kotlin
val unsafeTemperature= derivedStateFlow(States.temperature) { it > 40 }

monitor("UnsafeTemperatureSustained") {
    fireIf(unsafeTemperature.persistedFor(15.minutes))
    emits(Signal.UnsafeTemperature)
}
```

This monitor watches for sustained unsafe temperature. It will emit a signal only if the temperature remains above 40°F for 15 continuous minutes.

  </TabItem>
  <TabItem value="reaction" label="Reaction">

```kotlin
reaction("HandleUnsafeTemperature") {
    on(Signal.UnsafeTemperature)

    run { ctx ->
        ctx.schedule(Announce("Temperature too high. Food may be unsafe."))
        ctx.schedule(Actions.activateCooling)
    }
}
```

This reaction is subscribed to the signal and executes two actions when it's received: announce the event and shut off the cooling system.

  </TabItem>
  <TabItem value="action" label="Action">

```kotlin
object Actions {
    val activateCooling = action("ActivateCooling") {
        onExecute {
            States.coolingActive.value = true
            return ActionResult.Success("Cooling system turned on")
        }
    }
}
```

This action updates the system state directly. Actions are the final output of the execution loop and are free to mutate or communicate externally.

  </TabItem>
</Tabs>


## Next Steps

- Explore [Monitors](./concepts/monitor.md) in more depth
- Learn about [Temporal Expressions](./concepts/temporal.md)
- Review the [DSL Reference](./dsl/reference.md)
