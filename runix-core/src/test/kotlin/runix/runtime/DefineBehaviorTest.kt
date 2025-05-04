package runix.runtime

import io.mockk.*
import runix.primitives.action.ActionHandle
import runix.primitives.module.AppModule
import runix.primitives.monitor.MonitorHandle
import runix.primitives.reaction.ReactionHandle
import runix.runtime.internal.BehaviorRegistry
import kotlin.test.*

class DefineBehaviorTest {

    /**
     * Create a test module implementation to use in tests
     */
    private fun createTestModule(name: String = "TestModule"): AppModule {
        return mockk {
            every { this@mockk.name } returns name
        }
    }
    
    @Test
    fun `defineBehavior claims module in registry`() {
        val module = createTestModule()
        
        // Mock BehaviorRegistry to verify it's called
        mockkObject(BehaviorRegistry)
        every { BehaviorRegistry.claim(any()) } just Runs
        
        // Call defineBehavior
        module.defineBehavior {}
        
        // Verify registry claim was called with the module
        verify(exactly = 1) { BehaviorRegistry.claim(module) }
        
        unmockkObject(BehaviorRegistry)
    }
    
    @Test
    fun `defineBehavior invokes the provided block with scope`() {
        val module = createTestModule()
        var blockCalled = false
        
        // Call defineBehavior with a testable block
        module.defineBehavior {
            blockCalled = true
            // Verify "this" inside the block is a ModuleScope
            assertTrue(this is ModuleScope)
        }
        
        // Verify the block was called
        assertTrue(blockCalled)
    }

    @Test
    fun `defineBehavior activates scope after block execution`() {
        val module = createTestModule()

        // Track execution order
        val executionOrder = mutableListOf<String>()

        // Mock DefaultModuleScope 
        mockkConstructor(DefaultModuleScope::class)
        every { anyConstructed<DefaultModuleScope>().activate() } answers {
            executionOrder.add("activate")
        }

        // Call defineBehavior 
        module.defineBehavior {
            executionOrder.add("block")
        }

        // Verify activation happened after block execution
        assertEquals(listOf("block", "activate"), executionOrder)
    
        unmockkConstructor(DefaultModuleScope::class)
    }
    
    @Test
    fun `primitives added in block get registered during activation`() {
        val module = createTestModule()
        
        // Mock primitives
        val monitor = mockk<MonitorHandle>(relaxed = true)
        val reaction = mockk<ReactionHandle<*>>(relaxed = true)
        val action = mockk<ActionHandle<*>>(relaxed = true)
        
        // Call defineBehavior and add primitives
        module.defineBehavior {
            +monitor
            +reaction
            +action
        }
        
        // Verify the primitives were registered with the module
        verify { monitor.register(module) }
        verify { reaction.register(module) }
        verify { action.register(module) }
    }
    
    @Test
    fun `syntax sugar with plus operators works properly`() {
        val module = createTestModule()
        
        // Mock primitives
        val monitor = mockk<MonitorHandle>(relaxed = true)
        val reaction = mockk<ReactionHandle<*>>(relaxed = true)
        val action = mockk<ActionHandle<*>>(relaxed = true)
        
        // Call defineBehavior with the + operator syntax
        module.defineBehavior {
            +monitor
            +reaction
            +action
        }
        
        // Verify the primitives were registered
        verify { monitor.register(module) }
        verify { reaction.register(module) }
        verify { action.register(module) }
    }
    
    @Test
    fun `integration with BehaviorRegistry prevents duplicate calls`() {
        val module = createTestModule()
        
        // First call should succeed
        module.defineBehavior {}
        
        // Second call should throw because BehaviorRegistry will reject it
        val exception = assertFailsWith<IllegalStateException> {
            module.defineBehavior {}
        }
        
        // Verify the exception message contains the module name
        assertTrue(exception.message?.contains("TestModule") == true)
        assertTrue(exception.message?.contains("already defined") == true)
    }
}