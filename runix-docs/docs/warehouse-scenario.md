## Introduction

In a modern warehouse, hundreds of autonomous robots move through aisles, retrieve products, and return to charging stations. These robots operate under tight safety and coordination requirements — and every decision to move must be correct.

Consider one robot that detects a drivetrain fault while lifting a load. It halts immediately, entering an `EMERGENCY_STOP` state. Motion is disabled, power rails are isolated, and the fleet marks the robot as temporarily unavailable.

Resuming motion isn’t just a matter of “not being in an error state.” The system must ensure:

- The robot has **exited `EMERGENCY_STOP`**
- The **main 24V power rail** (which supplies drive systems and sensors) has been above 23.5 volts for **at least 5 seconds**
- The **IMU (inertial measurement unit)** and **LIDAR** sensors have not fluctuated in that time
- The robot has re-entered and held **`IDLE` mode** for at least **1 second**
- The robot was **performing a task recently** — this isn’t startup, it’s recovery

These requirements aren’t corner cases. They’re standard safety and recovery protocols in fleet robotics. But they’re notoriously hard to implement cleanly.

## The current reality

In most systems, these behaviors are cobbled together from:

- State machines tracking mode transitions
- Timers attached to sensor events
- Hand-coded debounce logic for each input
- Watchdog flags that track “readiness” by subtracting timestamps

Even well-structured systems suffer from fragility. It’s easy to:

- Resume too early because one flag was stale
- Miss a fluctuation window because data came late
- Forget to reset a timer when transitioning between states

The cognitive logic — the *meaning* of "safe to resume" — gets buried in glue code. And when something goes wrong, it's hard to explain why.

## A better model

What if you could express the condition directly?

Not as five flags and three callbacks — but as a single, declarative rule:

> The robot was recently active, exited `EMERGENCY_STOP`, all systems stabilized, and entered IDLE.

What if you could write that rule as a single object — and ask it whether it’s true?


## Our solution

The **Temporal Framework** lets you define **time-aware conditions** declaratively.

You describe what must be true — and for how long — and the framework:

- Observes state transitions
- Tracks numeric stability
- Maintains signal memory
- Evaluates readiness reactively

No timers. No queues. No manual resets. Just declarative cognition.


## What you write

```kotlin

val sensorsStable = not(
  anyOf(
    imu.gyroRate.hasFluctuatedBeyond(0.8, inLast = 3.seconds), // unexpected motion
    lidar.range.hasFluctuatedBeyond(0.5, inLast = 3.seconds) // noise in depth data
  )
)

val recoverySafeToResume = allOf(
  // The robot was recently:
  // performing a task -> stopped for an emergency -> went to idle
  robotMode.transitionedThrough(DELIVERING, EMERGENCY_STOP, IDLE, 
                                inLast = 30.seconds),
                             
  // IDLE was held
  robotMode.wasIn(IDLE, 
                  forDuration = 1.seconds, 
                  inLast = 10.seconds),

  // Power is stable
  power.mainBus.hasBeenAboveFor(23.5, forDuration = 5.seconds),

  // Sensors are calm
  sensorsStable
)
```

## What happens when you do

When this condition is assigned to a monitor, the system automatically tracks the changes in states, schedules checks, and retains transition information across time

You no longer need to write or manage:

- Transition flags
- Debounce timers
- Per-sensor recovery logic
- Cross-subsystem condition joiners

You express a condition that reflects **how the robot should behave** — and the system tracks it precisely. The moment the condition is met, the signal is emitted. You have confidence not just in the robot’s state — but in **how it got there**.
This is how your robot stops guessing — and starts reasoning.