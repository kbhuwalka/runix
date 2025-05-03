package runix.primitives.monitor

import runix.primitives.signal.SignalHandle
import runix.runtime.RuntimeScheduler
import runix.temporal.CompiledMonitor
import runix.temporal.MonitoredCondition
import runix.temporal.condition.ConditionEval
import java.util.concurrent.atomic.AtomicBoolean


/**
 * A declared monitor that observes a condition and optionally emits a signal.
 *
 * Monitors are created using the DSL:
 * ```
 * val batteryLow = monitor("batteryLow") {
 *     batteryLevel < 20.0
 * } emit batteryLowSignal
 * ```
 *
 * Registration must happen via `+batteryLow` inside a module `defineBehavior {}` block.
 *
 * Once registered and started, the monitor evaluates its condition in real-time and
 * can emit a signal when that condition evaluates to `true`.
 */
class MonitorHandle internal constructor(
    private val name: String,
    private val condition: MonitoredCondition,
    private val signal: SignalHandle<Unit>?
) {
    private var isRegistered = AtomicBoolean(false)
    private lateinit var compiled: CompiledMonitor
    private var started = AtomicBoolean(false)

    /**
     * Registers this monitor to a module.
     * This should only be called once per monitor instance.
     *
     * @param module Name of the module registering this monitor
     * @throws IllegalStateException if already registered
     */
    internal fun register(module: String) {
        check(!isRegistered.get()) {
            "Monitor '$name' is already registered to a module."
        }
        isRegistered.set(true)
        compiled = condition.compile(name)
    }

    internal fun start() {
        check(isRegistered.get()) { "Monitor '$name' must be registered before starting." }
        if (started.get()) return
        started.set(true)

        // Begin tracker observation and evaluation the pipeline
        compiled.start { evaluateAndMaybeEmit() }
        evaluateAndMaybeEmit() // Evaluate once at startup
    }

    /**
     * Stops the monitor and unregisters all observers.
     */
    internal fun stop() {
        if (!started.get()) return
        compiled.stop()
        RuntimeScheduler.cancelRecheck(this)
        started.set(false)
    }

    /**
     * Evaluates the current condition and emits a signal only if it is [ConditionEval.True].
     *
     * If [ConditionEval.Delayed], a recheck is scheduled.
     * If new values arrive before the recheck, it is canceled and reevaluated.
     */
    internal fun evaluateAndMaybeEmit() {
        val result = compiled.condition.invoke()

        RuntimeScheduler.cancelRecheck(this)
        when (result) {
            is ConditionEval.True -> signal?.emit(Unit)
            is ConditionEval.False -> {} // no-op
            is ConditionEval.Delayed -> RuntimeScheduler.scheduleRecheck(this, result.nextCheckAt) {
                evaluateAndMaybeEmit()
            }
        }
    }

    /**
     * Returns whether the monitor has been registered to a module.
     */
    internal fun isRegistered(): Boolean = isRegistered.get()

    override fun toString(): String = "Monitor($name)"
}