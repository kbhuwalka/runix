package runix.runtime.internal

import io.mockk.every
import io.mockk.mockk
import runix.primitives.module.AppModule
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BehaviorRegistryTest {

    /**
     * Test approach note: Since BehaviorRegistry is a stateful singleton,
     * and deliberately has no reset mechanism (as that would undermine its * guarantee), we must use unique module instances for each test to * avoid cross-test interference.
     */

    @Test
    fun `claim succeeds on first call for a module`() {
        val module = mockk<AppModule> {
            every { name } returns "FirstClaimModule"
        }

        // First claim should succeed without throwing
        BehaviorRegistry.claim(module)
    }

    @Test
    fun `claim throws on second call for same module`() {
        val module = mockk<AppModule> {
            every { name } returns "RepeatedClaimModule"
        }

        // First claim should succeed
        BehaviorRegistry.claim(module)

        // Second claim should throw
        val exception = assertFailsWith<IllegalStateException> {
            BehaviorRegistry.claim(module)
        }

        // Verify the error message includes the module name
        assertTrue(exception.message?.contains("RepeatedClaimModule") == true)
        assertTrue(exception.message?.contains("already defined") == true)
    }

    @Test
    fun `claim works for different modules`() {
        val module1 = mockk<AppModule> {
            every { name } returns "MultipleModule1"
        }

        val module2 = mockk<AppModule> {
            every { name } returns "MultipleModule2"
        }

        // Both claims should succeed
        BehaviorRegistry.claim(module1)
        BehaviorRegistry.claim(module2)
    }

    @Test
    fun `equality check uses module identity not just name`() {
        // Two different modules with the same name
        val module1 = mockk<AppModule> {
            every { name } returns "DuplicateName"
        }

        val module2 = mockk<AppModule> {
            every { name } returns "DuplicateName"
        }

        // Both claims should succeed as they're different module instances
        BehaviorRegistry.claim(module1)
        BehaviorRegistry.claim(module2)
    }
}
