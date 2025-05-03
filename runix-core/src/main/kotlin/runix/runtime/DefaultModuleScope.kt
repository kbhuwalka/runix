package runix.runtime

import runix.primitives.monitor.MonitorHandle
import runix.primitives.reaction.ReactionHandle
import runix.primitives.action.ActionHandle

/**
 * Internal runtime implementation of [ModuleScope].
 *
 * Accumulates declared primitives and registers them into the runtime scheduler.
 */
internal class DefaultModuleScope(
    private val moduleName: String
) : ModuleScope {

    private val pendingMonitors = mutableListOf<MonitorHandle>()
    private val pendingReactions = mutableListOf<ReactionHandle<*>>()
    private val pendingActions = mutableListOf<ActionHandle<*>>()

    // Track if activation has occurred
    private var isActivated = false

    override fun MonitorHandle.unaryPlus() {
        pendingMonitors += this
    }

    override fun ReactionHandle<*>.unaryPlus() {
        pendingReactions += this
    }

    override fun ActionHandle<*>.unaryPlus() {
        pendingActions += this
    }


    fun activate() {
        if (isActivated) return

        pendingMonitors.forEach { it.register(moduleName) }
        pendingReactions.forEach { it.register(moduleName) }
        pendingActions.forEach { it.register(moduleName) }
        isActivated = true
    }
}