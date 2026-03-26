---
id: action
title: Actions
sidebar_position: 5
---

# Actions

An action is a managed process. Use actions when work needs concurrency control, queuing, a timeout, or a structured result. Actions are how the system carries out behavior that has operational requirements beyond "just run this code."

## Declaring an action

### Typed input

```kotlin
fun <T> action(
    name: String,
    options: ActionOptions = ActionOptions(),
    block: suspend (T) -> ActionResult
): ActionHandle<T>
```

```kotlin
val moveToPosition = action<Coordinates>(
    name = "MoveToPosition",
    options = ActionOptions(
        timeout = 30.seconds,
        allowConcurrent = false,
        enqueueIfRunning = true
    )
) { coords ->
    robot.moveTo(coords)
    ActionResult.Success
}
```

### Zero input

```kotlin
fun action(
    name: String,
    options: ActionOptions = ActionOptions(),
    block: suspend () -> ActionResult
): ActionHandle<Unit>
```

```kotlin
val calibrateSensors = action("CalibrateSensors") {
    sensorArray.runCalibration()
    ActionResult.Success
}
```

Zero-input actions create an `ActionHandle<Unit>`. When calling them, pass `Unit`: `calibrateSensors.run(Unit)`.

## Running an action

```kotlin
suspend fun ActionHandle<T>.run(data: T): ActionResult
```

`run()` is suspending. It respects the action's concurrency policy and returns a structured result:

```kotlin
val result = moveToPosition.run(Coordinates(x = 1.0, y = 2.0))

when (result) {
    is ActionResult.Success -> logger.info("Moved successfully")
    is ActionResult.Failure -> logger.error("Move failed: ${result.cause}")
    is ActionResult.AlreadyRunning -> logger.warn("Move already in progress")
    is ActionResult.Timeout -> logger.warn("Move timed out after ${result.after}")
    is ActionResult.Cancelled -> logger.info("Move was cancelled")
}
```

## ActionOptions

```kotlin
data class ActionOptions(
    val timeout: Duration = Duration.INFINITE,
    val allowConcurrent: Boolean = false,
    val enqueueIfRunning: Boolean = false
)
```

**timeout**: The maximum time the action can run before being cancelled. Defaults to no timeout.

**allowConcurrent**: Whether multiple instances of this action can run at the same time. Defaults to `false`, meaning only one instance runs at a time.

**enqueueIfRunning**: When `allowConcurrent` is `false` and the action is already running, this controls what happens to new requests. If `true`, the request waits in a queue until the current run finishes. If `false`, it returns `ActionResult.AlreadyRunning` immediately.

## ActionResult

Every action returns one of these outcomes:

| Result | Meaning |
|---|---|
| `ActionResult.Success` | The action completed normally |
| `ActionResult.Failure(cause)` | The action threw an exception |
| `ActionResult.AlreadyRunning` | The action was rejected because it's already running and not configured to queue |
| `ActionResult.Cancelled` | The action was explicitly cancelled or its coroutine was cancelled |
| `ActionResult.Timeout(after)` | The action exceeded its timeout duration |

## Cancellation

Actions can be cancelled while running:

```kotlin
moveToPosition.cancel()     // cancel the currently running instance
moveToPosition.cancelAll()  // cancel running instance and all queued requests
```

## Checking status

```kotlin
if (moveToPosition.isRunning()) {
    // action is currently executing
}
```

## When to use an action

Actions add operational overhead that simple work doesn't need. Use them when at least one of these is true:

- The work takes measurable time (network calls, hardware commands, long computations)
- The work should not overlap with itself (concurrent robot movement commands)
- Callers need to know the outcome (success, failure, timeout)
- The work needs a timeout to prevent hanging

If none of these apply, do the work directly in a reaction instead.

---

→ [Getting Started](../getting-started.md): see all the primitives working together in a complete example

→ [The Cognitive Loop](../cognitive-loop.md): how monitors, signals, reactions, and actions connect