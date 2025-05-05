package runix.runtime

import runix.primitives.action.ActionHandle
import runix.primitives.monitor.MonitorHandle
import runix.primitives.reaction.ReactionHandle

/**
 * Internal runtime implementation of [ModuleScope].
 * 
 * Collects primitives registered during behavior definition
 * to be activated and managed by their owning module.
 */
internal class DefaultModuleScope : ModuleScope {
    private val pendingMonitors = mutableListOf<MonitorHandle>()
    private val pendingReactions = mutableListOf<ReactionHandle<*>>()
    private val pendingActions = mutableListOf<ActionHandle<*>>()

    override fun MonitorHandle.unaryPlus() {
        pendingMonitors += this
    }

    override fun ReactionHandle<*>.unaryPlus() {
        pendingReactions += this
    }

    override fun ActionHandle<*>.unaryPlus() {
        pendingActions += this
    }

    // Methods to retrieve collected primitives
    internal fun getMonitors(): List<MonitorHandle> = pendingMonitors
    internal fun getReactions(): List<ReactionHandle<*>> = pendingReactions
    internal fun getActions(): List<ActionHandle<*>> = pendingActions
}