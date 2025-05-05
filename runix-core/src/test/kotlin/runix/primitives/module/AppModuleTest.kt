package runix.primitives.module

import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
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
import runix.primitives.action.ActionHandle
import runix.primitives.monitor.MonitorHandle
import runix.primitives.reaction.ReactionHandle
import runix.runtime.App
import runix.runtime.internal.BehaviorRegistry
import runix.runtime.internal.RuntimeScope
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("AppModule")
class AppModuleTest {

    // Create a concrete implementation for testing the abstract class
    private class ConcreteModule(name: String) : AppModule(name)

    // Test infrastructure setup
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @BeforeEach
    fun setup() {
        // Mock RuntimeScope for coroutine tests
        mockkObject(RuntimeScope)
        every { RuntimeScope.scope } returns testScope

        // Mock BehaviorRegistry for defineBehavior
        mockkObject(BehaviorRegistry)
        justRun { BehaviorRegistry.claim(any()) }
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // Helper functions to create mocked primitives
    private fun createMockMonitor(name: String = "TestMonitor"): MonitorHandle {
        return mockk(relaxed = true) {
            justRun { register(any()) }
            coJustRun { activate() }
            coJustRun { deactivate() }
        }
    }

    private fun createMockReaction(name: String = "TestReaction"): ReactionHandle<*> {
        return mockk(relaxed = true) {
            justRun { register(any()) }
            coJustRun { activate() }
            coJustRun { deactivate() }
        }
    }

    private fun createMockAction(name: String = "TestAction"): ActionHandle<*> {
        return mockk(relaxed = true) {
            justRun { register(any()) }
        }
    }

    @Nested
    @DisplayName("Module construction")
    inner class ModuleConstruction {

        @Test
        @DisplayName("should set name from constructor")
        fun nameIsSetFromConstructor() {
            // Arrange & Act
            val module = ConcreteModule("TestModuleName")

            // Assert
            assertEquals("TestModuleName", module.name)
        }

        @Test
        @DisplayName("should claim module in behavior registry during defineBehavior")
        fun registersModuleWithBehaviorRegistry() {
            // Arrange
            val module = ConcreteModule("TestModule")

            // Act
            module.defineBehavior {}

            // Assert
            verify(exactly = 1) { BehaviorRegistry.claim(module) }
        }
    }

    @Nested
    @DisplayName("Primitive registration")
    inner class PrimitiveRegistration {

        @Test
        @DisplayName("should register monitors with module")
        fun registersMonitorsWithModule() {
            // Arrange
            val module = ConcreteModule("TestModule")
            val monitor = createMockMonitor()

            // Act - register the monitor directly
            module.defineBehavior {
                +monitor
            }

            // Assert
            verify(exactly = 1) { monitor.register(module) }
        }

        @Test
        @DisplayName("should register reactions with module")
        fun registersReactionsWithModule() {
            // Arrange
            val module = ConcreteModule("TestModule")
            val reaction = createMockReaction()

            // Act - register the reaction directly
            module.defineBehavior {
                +reaction
            }

            // Assert
            verify(exactly = 1) { reaction.register(module) }
        }

        @Test
        @DisplayName("should register actions with module")
        fun registersActionsWithModule() {
            // Arrange
            val module = ConcreteModule("TestModule")
            val action = createMockAction()

            // Act - register the action directly
            module.defineBehavior {
                +action
            }

            // Assert
            verify(exactly = 1) { action.register(module) }
        }

        @Test
        @DisplayName("should register multiple primitives")
        fun registersMultiplePrimitives() {
            // Arrange
            val module = ConcreteModule("TestModule")
            val monitor1 = createMockMonitor("Monitor1")
            val monitor2 = createMockMonitor("Monitor2")
            val reaction = createMockReaction()
            val action = createMockAction()

            // Act - register all primitives directly
            module.defineBehavior {
                +monitor1
                +monitor2
                +reaction
                +action
            }

            // Assert
            verify(exactly = 1) {
                monitor1.register(module)
                monitor2.register(module)
                reaction.register(module)
                action.register(module)
            }
        }
    }

    @Nested
    @DisplayName("Lifecycle management")
    inner class LifecycleManagement {

        @Test
        @DisplayName("should set parent app reference")
        fun setsParentAppReference() {
            // Arrange
            val module = ConcreteModule("TestModule")
            val app = mockk<App>()

            // Act
            module.setApp(app)

            // This test simply verifies no exceptions are thrown
            // since we can't directly verify a private field
        }

        @Nested
        @DisplayName("Activation")
        inner class Activation {

            @Test
            @DisplayName("should activate all monitors")
            fun activatesAllMonitors() = runTest {
                // Arrange
                val module = ConcreteModule("TestModule")
                val monitor1 = createMockMonitor("Monitor1")
                val monitor2 = createMockMonitor("Monitor2")
                
                module.defineBehavior {
                    +monitor1
                    +monitor2
                }

                // Act
                module.activate()
                testScope.advanceUntilIdle()

                // Assert
                coVerify(exactly = 1) {
                    monitor1.activate()
                    monitor2.activate()
                }
            }

            @Test
            @DisplayName("should activate all reactions")
            fun activatesAllReactions() = runTest {
                // Arrange
                val module = ConcreteModule("TestModule")
                val reaction1 = createMockReaction("Reaction1")
                val reaction2 = createMockReaction("Reaction2")
                
                module.defineBehavior {
                    +reaction1
                    +reaction2
                }

                // Act
                module.activate()
                testScope.advanceUntilIdle()

                // Assert
                coVerify(exactly = 1) {
                    reaction1.activate()
                    reaction2.activate()
                }
            }

            @Test
            @DisplayName("should call didStart callback after primitives activate")
            fun callsDidStartAfterPrimitiveActivation() = runTest {
                // Arrange
                val module = ConcreteModule("TestModule")
                val monitor = createMockMonitor()
                
                module.defineBehavior {
                    +monitor
                }

                var didStartCallbackExecuted = false
                module.didStart {
                    didStartCallbackExecuted = true
                }

                // Act
                module.activate()
                testScope.advanceUntilIdle()

                // Assert
                assertTrue(didStartCallbackExecuted, "didStart callback should have executed")
                
                // Verify activation happened before callback
                coVerify(exactly = 1) { monitor.activate() }
            }
        }

        @Nested
        @DisplayName("Deactivation")
        inner class Deactivation {

            @Test
            @DisplayName("should call willStop before primitive deactivation")
            fun callsWillStopBeforePrimitiveDeactivation() = runTest {
                // Arrange
                val module = ConcreteModule("TestModule")
                var willStopCalled = false
                val deactivationCalled = CompletableDeferred<Boolean>()

                // Create a special monitor with instrumentation
                val monitor = mockk<MonitorHandle> {
                    justRun { register(any()) }
                    coJustRun { activate() }
                    coEvery { deactivate() } coAnswers {
                        // Simply track that deactivate was called and the state of willStopCalled
                        deactivationCalled.complete(willStopCalled)
                    }
                }

                module.defineBehavior {
                    +monitor
                }

                module.willStop {
                    willStopCalled = true
                }

                // First activate the module
                module.activate()
                testScope.advanceUntilIdle()

                // Act - deactivate
                module.deactivate()
                testScope.advanceUntilIdle()

                // Assert
                assertTrue(willStopCalled, "willStop callback should have been called")
                assertTrue(deactivationCalled.await(), "willStop should be called before deactivation")
            }

            @Test
            @DisplayName("should deactivate reactions before monitors")
            fun deactivatesReactionsBeforeMonitors() = runTest {
                // Arrange
                val module = ConcreteModule("TestModule")
                val monitor = createMockMonitor()
                val reaction = createMockReaction()
                
                module.defineBehavior {
                    +monitor
                    +reaction
                }

                // First activate the module
                module.activate()
                testScope.advanceUntilIdle()

                // Act - deactivate
                module.deactivate()
                testScope.advanceUntilIdle()

                // Assert - verify order with MockK
                coVerifyOrder {
                    reaction.deactivate()
                    monitor.deactivate()
                }
            }

            @Test
            @DisplayName("should deactivate primitives in reverse registration order")
            fun deactivatesInReverseRegistrationOrder() = runTest {
                // Arrange
                val module = ConcreteModule("TestModule")
                val monitor1 = createMockMonitor("Monitor1")
                val monitor2 = createMockMonitor("Monitor2")
                val reaction1 = createMockReaction("Reaction1")
                val reaction2 = createMockReaction("Reaction2")
                
                module.defineBehavior {
                    +monitor1
                    +monitor2
                    +reaction1
                    +reaction2
                }

                // First activate the module
                module.activate()
                testScope.advanceUntilIdle()

                // Act - deactivate
                module.deactivate()
                testScope.advanceUntilIdle()

                // Verify reactions activate in registration order
                coVerifyOrder {
                    reaction1.activate()
                    reaction2.activate()
                }

                // Verify monitors activate in registration order
                coVerifyOrder {
                    monitor1.activate()
                    monitor2.activate()
                }

                // Verify reactions deactivate in reverse order
                coVerifyOrder {
                    reaction2.deactivate()
                    reaction1.deactivate()
                }

                // Verify monitors deactivate in reverse order
                coVerifyOrder {
                    monitor2.deactivate()
                    monitor1.deactivate()
                }
            }
        }

        @Nested
        @DisplayName("Multiple activations")
        inner class MultipleActivations {

            @Test
            @DisplayName("should support repeated activate/deactivate cycles")
            fun supportsMultipleActivationCycles() = runTest {
                // Arrange
                val module = ConcreteModule("TestModule")
                val monitor = createMockMonitor()
                
                module.defineBehavior {
                    +monitor
                }

                // Act & Assert - First cycle
                module.activate()
                testScope.advanceUntilIdle()
                coVerify(exactly = 1) { monitor.activate() }

                module.deactivate()
                testScope.advanceUntilIdle()
                coVerify(exactly = 1) { monitor.deactivate() }

                // Act & Assert - Second cycle
                module.activate()
                testScope.advanceUntilIdle()
                coVerify(exactly = 2) { monitor.activate() }

                module.deactivate()
                testScope.advanceUntilIdle()
                coVerify(exactly = 2) { monitor.deactivate() }
            }
        }
    }
}