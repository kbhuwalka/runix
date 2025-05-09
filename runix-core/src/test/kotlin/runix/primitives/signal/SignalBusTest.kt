package runix.primitives.signal

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
import runix.RuntimeScopeTestHelper
import runix.primitives.reaction.ReactionHandle
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("SignalBus")
class SignalBusTest {

    // Test infrastructure setup
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val runtimeHelper = RuntimeScopeTestHelper(testScope)

    @BeforeEach
    fun setup() {
        // Setup RuntimeScope mocking with the helper
        runtimeHelper.setup()

        // Reset SignalBus state before each test
        SignalBus.reset()
    }

    @AfterEach
    fun tearDown() {
        runtimeHelper.tearDown()
        SignalBus.reset()
    }

    // Helper functions to create mocked reaction for String type
    private fun createStringReaction(
        name: String = "testReaction",
        handler: suspend (String) -> Unit = {}
    ): ReactionHandle<String> {
        return mockk {
            every { this@mockk.name } returns name
            coEvery { this@mockk.handler(any()) } coAnswers { handler(firstArg()) }
        }
    }

    // Helper functions to create mocked reaction for Int type
    private fun createIntReaction(
        name: String = "testReaction",
        handler: suspend (Int) -> Unit = {}
    ): ReactionHandle<Int> {
        return mockk {
            every { this@mockk.name } returns name
            coEvery { this@mockk.handler(any()) } coAnswers { handler(firstArg()) }
        }
    }

    @Nested
    @DisplayName("Signal emission and reaction handling")
    inner class SignalEmissionAndReactions {

        @Test
        @DisplayName("should invoke registered reactions when a signal is emitted")
        fun invokesRegisteredReactionsOnEmit() = runTest {
            // Arrange
            val signal = SignalHandle<String>("testSignal")
            val value = "test value"
            var reaction1Called = false
            var reaction2Called = false

            val reaction1 = createStringReaction("reaction1") {
                if (it == value) reaction1Called = true
            }

            val reaction2 = createStringReaction("reaction2") {
                if (it == value) reaction2Called = true
            }

            // Register reactions
            SignalBus.register(signal, reaction1)
            SignalBus.register(signal, reaction2)

            // Act
            SignalBus.emit(signal, value)
            testScope.advanceUntilIdle()

            // Assert
            assertTrue(reaction1Called, "Reaction1 should be called")
            assertTrue(reaction2Called, "Reaction2 should be called")
            coVerify(exactly = 1) { reaction1.handler(value) }
            coVerify(exactly = 1) { reaction2.handler(value) }
        }

        @Test
        @DisplayName("should not invoke unregistered reactions")
        fun doesNotInvokeUnregisteredReactions() = runTest {
            // Arrange
            val signal = SignalHandle<String>("testSignal")
            val value = "test value"
            var reactionCalled = false

            val reaction = createStringReaction { reactionCalled = true }

            // Register and then unregister the reaction
            SignalBus.register(signal, reaction)
            SignalBus.unregister(signal, reaction)

            // Act
            SignalBus.emit(signal, value)
            testScope.advanceUntilIdle()

            // Assert
            assertFalse(reactionCalled, "Reaction should not be called after unregistration")
            coVerify(exactly = 0) { reaction.handler(any()) }
        }

        @Test
        @DisplayName("should continue processing other reactions when one throws an exception")
        fun continuesProcessingAfterException() = runTest {
            // Arrange
            val signal = SignalHandle<String>("testSignal")
            val value = "test value"
            var errorReactionCalled = false
            var successReactionCalled = false

            val errorReaction = createStringReaction("errorReaction") {
                errorReactionCalled = true
                throw RuntimeException("Test exception")
            }

            val successReaction = createStringReaction("successReaction") {
                successReactionCalled = true
            }

            SignalBus.register(signal, errorReaction)
            SignalBus.register(signal, successReaction)

            // Act - should not throw exception up to the caller
            SignalBus.emit(signal, value)
            testScope.advanceUntilIdle()

            // Assert
            assertTrue(errorReactionCalled, "Error reaction should be called")
            assertTrue(successReactionCalled, "Success reaction should still be called")
        }

        @Test
        @DisplayName("should not invoke reactions for unrelated signals")
        fun doesNotInvokeReactionsForUnrelatedSignals() = runTest {
            // Arrange
            val signal1 = SignalHandle<String>("signal1")
            val signal2 = SignalHandle<String>("signal2")
            var reaction1Called = false
            var reaction2Called = false

            val reaction1 = createStringReaction { reaction1Called = true }
            val reaction2 = createStringReaction { reaction2Called = true }

            SignalBus.register(signal1, reaction1)
            SignalBus.register(signal2, reaction2)

            // Act - emit only signal1
            SignalBus.emit(signal1, "test")
            testScope.advanceUntilIdle()

            // Assert
            assertTrue(reaction1Called, "Reaction1 should be called")
            assertFalse(reaction2Called, "Reaction2 should not be called")
        }

        @Test
        @DisplayName("should respect type safety of signals and reactions")
        fun respectsTypeSafety() = runTest {
            // Arrange
            val stringSignal = SignalHandle<String>("stringSignal")
            val intSignal = SignalHandle<Int>("intSignal")

            var stringReactionCalled = false
            var intReactionCalled = false

            val stringReaction = createStringReaction { stringReactionCalled = true }
            val intReaction = createIntReaction { intReactionCalled = true }

            SignalBus.register(stringSignal, stringReaction)
            SignalBus.register(intSignal, intReaction)

            // Act
            SignalBus.emit(stringSignal, "test")
            testScope.advanceUntilIdle()

            // Assert
            assertTrue(stringReactionCalled, "String reaction should be called")
            assertFalse(intReactionCalled, "Int reaction should not be called")
        }
    }

    @Nested
    @DisplayName("Lifecycle management")
    inner class LifecycleManagement {

        @Test
        @DisplayName("should be able to reset all registrations")
        fun resetsAllRegistrations() = runTest {
            // Arrange
            val signal = SignalHandle<String>("testSignal")
            var reactionCalled = false

            val reaction = createStringReaction { reactionCalled = true }

            SignalBus.register(signal, reaction)

            // Act
            SignalBus.reset()
            SignalBus.emit(signal, "test")
            testScope.advanceUntilIdle()

            // Assert
            assertFalse(reactionCalled, "Reaction should not be called after reset")
            coVerify(exactly = 0) { reaction.handler(any()) }
        }

        @Test
        @DisplayName("should handle multiple registrations of the same reaction")
        fun handlesMultipleRegistrationsOfSameReaction() = runTest {
            // Arrange
            val signal = SignalHandle<String>("testSignal")
            var callCount = 0

            val reaction = createStringReaction { callCount++ }

            // Act - register multiple times
            SignalBus.register(signal, reaction)
            SignalBus.register(signal, reaction) // Register again

            SignalBus.emit(signal, "test")
            testScope.advanceUntilIdle()

            // Assert - should only be called once
            coVerify(exactly = 1) { reaction.handler(any()) }
        }

        @Test
        @DisplayName("should allow unregistering a reaction that wasn't registered")
        fun allowsUnregisteringNonExistentReaction() = runTest {
            // Arrange
            val signal = SignalHandle<String>("testSignal")
            val reaction = createStringReaction()

            // Act - should not throw
            SignalBus.unregister(signal, reaction)

            // No assertions - test passes if no exception is thrown
        }
    }
}