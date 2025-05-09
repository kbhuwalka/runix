package runix.temporal

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import runix.runtime.internal.RuntimeScope
import runix.temporal.condition.TemporalExpression
import runix.temporal.trackers.TrackerRegistry

/**
 * A compiled, executable cognitive monitor built from a [MonitoredCondition].
 *
 * A `CompiledMonitor`:
 * - Holds the compiled [TemporalExpression] that represents the condition's logic
 * - Manages lifecycle (`start`, `stop`) of all involved trackers
 * - Connects signal changes to condition reevaluation via [onUpdate]
 *
 * Monitors are created by calling `.compile("monitorName")` on a [MonitoredCondition].
 * They are inactive by default and must be started explicitly using [start].
 *
 * 🧠 Cognition Contract:
 * - Evaluates to a [ConditionEval] at any time
 * - Tracks only within the defined memory/retention scope
 * - Executes all signal observations reactively
 */
internal data class CompiledMonitor(
    val name: String,
    val condition: TemporalExpression,
    private val bindings: List<FlowBinding<*>>
) {
    private var started = false
    private val flowUpdateJobs = mutableListOf<Job>()

    fun start(onUpdate: () -> Unit) {
        if (started) return
        started = true

        for (binding in bindings) {
            val tracker = binding.tracker
            tracker.registerWith(binding.key)
        }

        val uniqueFlows = bindings.mapTo(mutableSetOf()) { it.sourceFlow }

        uniqueFlows.forEach { flow ->
            val job  = RuntimeScope.scope.launch {
                flow.collect {
                    onUpdate()
                }
            }
            flowUpdateJobs.add(job)
        }
    }

    fun stop() {
        if (!started) return
        started = false

        bindings.forEach { TrackerRegistry.unregister(it.key) }
        flowUpdateJobs.forEach { it.cancel() }
        flowUpdateJobs.clear()
    }
}
