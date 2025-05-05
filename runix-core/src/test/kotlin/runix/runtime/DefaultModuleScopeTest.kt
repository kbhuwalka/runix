package runix.runtime

import io.mockk.mockk
import org.junit.jupiter.api.Test
import runix.primitives.action.ActionHandle
import runix.primitives.monitor.MonitorHandle
import runix.primitives.reaction.ReactionHandle
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultModuleScopeTest {

    @Test
    fun `constructor initializes empty collections`() {
        val scope = DefaultModuleScope()

        assertTrue(scope.getMonitors().isEmpty())
        assertTrue(scope.getReactions().isEmpty())
        assertTrue(scope.getActions().isEmpty())
    }

    @Test
    fun `plus operator adds MonitorHandle to pending list`() {
        val scope = DefaultModuleScope()
        val monitor1 = mockk<MonitorHandle>(relaxed = true)
        val monitor2 = mockk<MonitorHandle>(relaxed = true)

        with(scope) {
            +monitor1
            +monitor2
        }

        val monitors = scope.getMonitors()
        assertEquals(2, monitors.size)
        assertEquals(monitor1, monitors[0])
        assertEquals(monitor2, monitors[1])
    }

    @Test
    fun `plus operator adds ReactionHandle to pending list`() {
        val scope = DefaultModuleScope()
        val reaction1 = mockk<ReactionHandle<Unit>>(relaxed = true)
        val reaction2 = mockk<ReactionHandle<Unit>>(relaxed = true)

        with(scope) {
            +reaction1
            +reaction2
        }

        val reactions = scope.getReactions()
        assertEquals(2, reactions.size)
        assertEquals(reaction1, reactions[0])
        assertEquals(reaction2, reactions[1])
    }

    @Test
    fun `plus operator adds ActionHandle to pending list`() {
        val scope = DefaultModuleScope()
        val action1 = mockk<ActionHandle<*>>(relaxed = true)
        val action2 = mockk<ActionHandle<*>>(relaxed = true)

        with(scope) {
            +action1
            +action2
        }

        val actions = scope.getActions()
        assertEquals(2, actions.size)
        assertEquals(action1, actions[0])
        assertEquals(action2, actions[1])
    }

    @Test
    fun `mixed primitives are stored in type-specific collections`() {
        val scope = DefaultModuleScope()

        // Create mocks for each type
        val monitor = mockk<MonitorHandle>(relaxed = true)
        val reaction = mockk<ReactionHandle<Unit>>(relaxed = true)
        val action = mockk<ActionHandle<*>>(relaxed = true)

        // Add them in mixed order
        with(scope) {
            +monitor
            +action
            +reaction
        }

        // Verify each is in its own collection
        assertEquals(listOf(monitor), scope.getMonitors())
        assertEquals(listOf(reaction), scope.getReactions())
        assertEquals(listOf(action), scope.getActions())
    }

    @Test
    fun `get methods work with empty collections`() {
        val scope = DefaultModuleScope()

        assertTrue(scope.getMonitors().isEmpty())
        assertTrue(scope.getReactions().isEmpty())
        assertTrue(scope.getActions().isEmpty())
    }

    @Test
    fun `plus supports multiple primitives of the same type`() {
        val scope = DefaultModuleScope()

        // Create multiple primitives of each type
        val monitors = List(3) { mockk<MonitorHandle>(relaxed = true) }
        val reactions = List(2) { mockk<ReactionHandle<*>>(relaxed = true) }
        val actions = List(4) { mockk<ActionHandle<*>>(relaxed = true) }

        // Add all primitives using with block
        with(scope) {
            monitors.forEach { +it }
            reactions.forEach { +it }
            actions.forEach { +it }
        }

        // Verify all were stored in their respective collections
        assertEquals(monitors, scope.getMonitors())
        assertEquals(reactions, scope.getReactions())
        assertEquals(actions, scope.getActions())
    }

    @Test
    fun `adding the same primitive multiple times adds multiple instances`() {
        val scope = DefaultModuleScope()
        val monitor = mockk<MonitorHandle>(relaxed = true)

        with(scope) {
            +monitor
            +monitor
            +monitor
        }

        val monitors = scope.getMonitors()
        assertEquals(3, monitors.size)
        assertEquals(monitor, monitors[0])
        assertEquals(monitor, monitors[1])
        assertEquals(monitor, monitors[2])
    }

    @Test
    fun `collections are not shared between scope instances`() {
        val scope1 = DefaultModuleScope()
        val scope2 = DefaultModuleScope()

        val monitor1 = mockk<MonitorHandle>(relaxed = true)
        val monitor2 = mockk<MonitorHandle>(relaxed = true)

        with(scope1) { +monitor1 }
        with(scope2) { +monitor2 }

        // Verify scope1 only has monitor1
        assertEquals(listOf(monitor1), scope1.getMonitors())
        assertTrue(scope1.getReactions().isEmpty())
        assertTrue(scope1.getActions().isEmpty())

        // Verify scope2 only has monitor2
        assertEquals(listOf(monitor2), scope2.getMonitors())
        assertTrue(scope2.getReactions().isEmpty())
        assertTrue(scope2.getActions().isEmpty())
    }
}