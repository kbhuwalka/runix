---
id: dsl-reference
title: DSL Reference
---

## monitor(name)

Defines a monitor block.

```kotlin
monitor("Name") {
    dependsOn(...)
    condition { ... }
    trigger(Signal.Name)
}
```

## reaction(name)

Defines a reaction to signals.

```kotlin
reaction("Name") {
    on(Signal.Name)
    run { ctx ->
        ctx.scheduleChild(Announce("Hello!"))
    }
}
```
