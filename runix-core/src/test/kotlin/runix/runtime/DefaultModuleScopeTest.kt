package runix.runtime

import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import runix.primitives.action.ActionHandle
import runix.primitives.monitor.MonitorHandle
import runix.primitives.reaction.ReactionHandle
import kotlin.test.Test

class DefaultModuleScopeTest {

    private val testModuleName = "TestModule"
    
    @Test
    fun `constructor properly initializes moduleName`() {
        val scope = DefaultModuleScope(testModuleName)
        
        // This is an indirect test via the activate method, since moduleName is private
        val mockMonitor = mockk<MonitorHandle>(relaxed = true)
        with(scope) {
            +mockMonitor
        }
        scope.activate()
        
        verify { mockMonitor.register(testModuleName) }
    }

    @Test
    fun `plus operator adds MonitorHandle to pending list`() {
        val scope = DefaultModuleScope(testModuleName)
        val monitor1 = mockk<MonitorHandle>(relaxed = true)
        val monitor2 = mockk<MonitorHandle>(relaxed = true)

        with(scope) {
            +monitor1
            +monitor2
        }

        scope.activate()
        
        // Verify both were registered in the order they were added
        verifySequence {
            monitor1.register(testModuleName)
            monitor2.register(testModuleName)
        }
    }
    
    @Test
    fun `plus operator adds ReactionHandle to pending list`() {
        val scope = DefaultModuleScope(testModuleName)
        val reaction1 = mockk<ReactionHandle<Unit>>(relaxed = true)
        val reaction2 = mockk<ReactionHandle<Unit>>(relaxed = true)
        
        with(scope) {
            +reaction1
            +reaction2
        }
        scope.activate()
        
        // Verify both were registered in the order they were added
        verifySequence {
            reaction1.register(testModuleName)
            reaction2.register(testModuleName)
        }
    }
    
    @Test
    fun `plus operator adds ActionHandle to pending list`() {
        val scope = DefaultModuleScope(testModuleName)
        val action1 = mockk<ActionHandle<*>>(relaxed = true)
        val action2 = mockk<ActionHandle<*>>(relaxed = true)

        with(scope) {
            +action1
            +action2
        }
        scope.activate()
        
        // Verify both were registered in the order they were added
        verifySequence {
            action1.register(testModuleName)
            action2.register(testModuleName)
        }
    }

    @Test
    fun `activate registers all pending primitives in the correct order`() {
        val scope = DefaultModuleScope(testModuleName)
        
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
        
        // Activate should register them by type (monitors, reactions, actions)
        scope.activate()
        
        // Verify registration order by type
        verifySequence {
            monitor.register(testModuleName)
            reaction.register(testModuleName)
            action.register(testModuleName)
        }
    }
    
    @Test
    fun `activate works with empty lists`() {
        val scope = DefaultModuleScope(testModuleName)
        
        // Should not throw any exceptions
        scope.activate()
    }
    
    @Test
    fun `plus supports multiple primitives of the same type`() {
        val scope = DefaultModuleScope(testModuleName)

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

        // Activate
        scope.activate()

        // Verify all were registered in the expected type order
        verifySequence {
            monitors.forEach { it.register(testModuleName) }
            reactions.forEach { it.register(testModuleName) }
            actions.forEach { it.register(testModuleName) }
        }
    }
    
    @Test
    fun `multiple activate calls register primitives only once`() {
        val scope = DefaultModuleScope(testModuleName)
        val monitor = mockk<MonitorHandle>(relaxed = true)
        
        with(scope) {
            +monitor
        }
        
        // Call activate multiple times
        scope.activate()
        scope.activate()
        scope.activate()
        
        // Verify registration was only called once
        verify(exactly = 1) { monitor.register(testModuleName) }
    }
}