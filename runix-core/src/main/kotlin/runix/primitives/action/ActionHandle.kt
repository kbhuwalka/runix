package runix.primitives.action

import kotlinx.coroutines.withContext
import runix.primitives.module.AppModule
import runix.runtime.internal.Registerable
import runix.runtime.internal.RegistrationGuard
import runix.temporal.time.Time
import runix.tracing.TraceCollector
import runix.tracing.TraceContext
import runix.tracing.TraceContextElement
import runix.tracing.currentOrRoot
import runix.tracing.events.ActionRequested
import kotlin.time.Duration

/**
 * Represents a coroutine-based action that can be invoked in a Runix application.
 *
 * Actions encapsulate asynchronous logic with configurable execution semantics:
 * - Concurrency: Whether multiple runs may happen simultaneously
 * - Queuing: Whether additional calls queue while one is running
 * - Timeout: Whether to cancel an action after a duration
 *
 * Use [run] to execute the action, respecting all flags.
 */
class ActionHandle<T>(
    val name: String,
    private val allowConcurrent: Boolean,
    private val enqueueIfRunning: Boolean,
    timeout: Duration,
    block: suspend (T) -> ActionResult
) : Registerable {
    private val queue: ActionQueue<T> = ActionQueue(name, timeout, allowConcurrent, enqueueIfRunning, block)
    private val guard = RegistrationGuard()

    /**
     * Registers this action with the framework under the given module name.
     *
     * This enables traceability, diagnostics, and integration with tooling.
     * Must be called exactly once during `defineBehavior`.
     *
     * @param module the name of the module registering this action
     * @throws IllegalStateException if already registered
     */
    override fun register(module: AppModule) {
        guard.register(module)
    }

    /**
     * True if any instances of this action are currently running.
     */
    fun isRunning(): Boolean = queue.isAnyJobActive()

    /**
     * Runs this action with the provided input [data], respecting the concurrency policy:
     *
     * - If [allowConcurrent] is false and the action is already running:
     *   - If [enqueueIfRunning] is true, the call will suspend until prior executions complete.
     *   - If false, returns [ActionResult.AlreadyRunning] immediately.
     *
     * This method is **suspendable** and ensures that only one execution path
     * for this action is active at a time unless explicitly allowed.
     */
    suspend fun run(data: T): ActionResult {
        val requested = Time.markNow()
        val actionRequestedTrace = TraceContext.currentOrRoot()
        TraceCollector.emit(
            ActionRequested(
                traceId = actionRequestedTrace.traceId,
                parentId = actionRequestedTrace.parentId,
                actionName = name
            )
        )

        val result =  withContext(TraceContextElement(actionRequestedTrace)) {
            queue.submit(data)
        }

        val resultTrace = actionRequestedTrace.derive()
        TraceCollector.emit(
            result.toTraceEvent(resultTrace, name, requested.elapsedNow())
        )

        return result
    }

    fun cancel() {
        queue.cancelRunning()
    }

    fun cancelAll() {
        queue.cancelAll()
    }

    override fun toString(): String = "Action($name)"
}
