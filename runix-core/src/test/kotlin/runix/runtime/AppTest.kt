package runix.runtime

import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import runix.RuntimeScopeTestHelper
import runix.primitives.module.AppModule
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("App")
class AppTest {

    // Test infrastructure setup
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val runtimeHelper = RuntimeScopeTestHelper(testScope)

    @BeforeEach
    fun setUp() {
        runtimeHelper.setup()
    }

    @AfterEach
    fun tearDown() {
        runtimeHelper.tearDown()
    }

    // Helper functions to create test components
    private fun createMockModule(
        name: String = "TestModule",
        failOnActivate: Boolean = false
    ): AppModule {
        return mockk<AppModule>(relaxed = true) {
            every { this@mockk.name } returns name
            if (failOnActivate) {
                coEvery { activate() } throws RuntimeException("Activation failed")
            } else {
                coJustRun { activate() }
            }
            coJustRun { deactivate() }
        }
    }

    @Nested
    @DisplayName("Module installation")
    inner class ModuleInstallation {

        @Test
        @DisplayName("should add module to app and set app reference")
        fun addModuleAndSetAppReference() {
            // Arrange
            val app = App()
            val module = createMockModule()

            // Act
            app.install(module)

            // Assert
            verify { module.setApp(app) }
            assertTrue(app.testModules.contains(module))
        }
    }

    @Nested
    @DisplayName("Lifecycle management")
    inner class LifecycleManagement {

        @Nested
        @DisplayName("Activation")
        inner class Activation {

            @Test
            @DisplayName("should activate all modules and call didStart")
            fun activatesModulesAndCallsDidStart() = runTest {
                // Arrange
                val app = App()
                var didStartCalled = false
                
                // Create and install mock modules
                val modules = List(3) { index ->
                    createMockModule("Module$index")
                }
                modules.forEach { app.install(it) }
                
                app.didStart { didStartCalled = true }

                // Act
                app.activate()
                advanceUntilIdle()

                // Assert - verify all modules were activated
                modules.forEach { coVerify { it.activate() } }
                
                // Verify didStart was called
                assertTrue(didStartCalled, "didStart callback should be called")
            }

            @Test
            @DisplayName("should be idempotent")
            fun activationIsIdempotent() = runTest {
                // Arrange
                val app = App()
                val module = createMockModule()
                app.install(module)

                // Act - call activate twice
                app.activate()
                advanceUntilIdle()
                app.activate() // Second call should be a no-op
                advanceUntilIdle()

                // Assert - module's activate should only be called once
                coVerify(exactly = 1) { module.activate() }
            }

            @Test
            @DisplayName("should propagate module activation errors")
            fun propagatesModuleActivationErrors() = runTest {
                // Arrange
                val app = App()
                val goodModule = createMockModule("GoodModule")
                val errorModule = createMockModule("ErrorModule", failOnActivate = true)
                
                app.install(goodModule)
                app.install(errorModule)
                
                var didStartCalled = false
                app.didStart { didStartCalled = true }

                // Act & Assert - activation should fail
                assertThrows<RuntimeException> {
                    app.activate()
                    advanceUntilIdle()
                }
                
                // didStart shouldn't be called due to activation failure
                assertFalse(didStartCalled, "didStart shouldn't be called if activation fails")
            }
        }

        @Nested
        @DisplayName("Deactivation")
        inner class Deactivation {

            @Test
            @DisplayName("should call willStop and deactivate modules in reverse order")
            fun callsWillStopAndDeactivatesInReverseOrder() = runTest {
                // Arrange
                val app = App()
                var willStopCalled = false
                val deactivationOrder = mutableListOf<String>()
                
                // Create mock modules that track deactivation order
                val modules = List(3) { index ->
                    mockk<AppModule>(relaxed = true) {
                        every { name } returns "Module$index"
                        coJustRun { activate() }
                        coEvery { deactivate() } coAnswers {
                            deactivationOrder.add("Module$index")
                        }
                    }
                }
                
                // Install modules and set lifecycle callback
                modules.forEach { app.install(it) }
                app.willStop { willStopCalled = true }
                
                // First activate
                app.activate()
                advanceUntilIdle()
                
                // Act - deactivate
                app.deactivate()
                advanceUntilIdle()
                
                // Assert
                assertTrue(willStopCalled, "willStop callback should be called")
                
                // Verify deactivation order (reverse of installation)
                assertEquals(
                    listOf("Module2", "Module1", "Module0"),
                    deactivationOrder,
                    "Modules should be deactivated in reverse order"
                )
            }

            @Test
            @DisplayName("should be idempotent")
            fun deactivationIsIdempotent() = runTest {
                // Arrange
                val app = App()
                val module = createMockModule()
                app.install(module)
                
                // First activate
                app.activate()
                advanceUntilIdle()
                
                // Act - call deactivate twice
                app.deactivate()
                advanceUntilIdle()
                app.deactivate() // Second call should be a no-op
                advanceUntilIdle()
                
                // Assert - module's deactivate should only be called once
                coVerify(exactly = 1) { module.deactivate() }
            }
        }
    }

    /**
     * Concrete implementation for testing purposes
     */
    private class App : runix.runtime.App() {
        // Expose modules for testing
        val testModules: List<AppModule>
            get() = this.modules.toList()
    }
}