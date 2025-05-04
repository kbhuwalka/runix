package runix.runtime

import runix.primitives.action.ActionHandle
import runix.primitives.module.AppModule
import runix.primitives.monitor.MonitorHandle
import runix.primitives.reaction.ReactionHandle

/**
 * Internal runtime implementation of [ModuleScope].
 *
 * Accumulates declared primitives and registers them into the runtime scheduler.
 */
internal class DefaultModuleScope(
    private val module: AppModule
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

        pendingMonitors.forEach { it.register(module) }
        pendingReactions.forEach { it.register(module) }
        pendingActions.forEach { it.register(module) }
        isActivated = true
    }
}
