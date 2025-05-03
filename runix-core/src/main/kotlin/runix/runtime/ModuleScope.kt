package runix.runtime

import runix.primitives.action.ActionHandle
import runix.primitives.monitor.MonitorHandle
import runix.primitives.reaction.ReactionHandle

/**
 * Provides a scoped registration context for behavior primitives.
 *
 * Each [AppModule] is given a [ModuleScope] during [defineBehavior],
 * which is the only time behavior may be registered.
 *
 * All monitors, actions, and reactions must be registered here to be valid.
 */
interface ModuleScope {

    /**
     * Registers a monitor in this module.
     */
    operator fun MonitorHandle.unaryPlus()

    /**
     * Registers a reaction in this module.
     */
    operator fun ReactionHandle.unaryPlus()

    /**
     * Registers an action in this module.
     */
    operator fun ActionHandle<*>.unaryPlus()

}