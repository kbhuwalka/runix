package runix.primitives.signal

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import runix.primitives.reaction.reaction
import runix.runtime.internal.RuntimeScope
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("SignalBus")
class SignalBusTest {

    // Test infrastructure setup
    private val scheduler = TestCoroutineScheduler()
    private val testScope = TestScope(scheduler)

    @BeforeEach
    fun setup() {
        // Setup RuntimeScope for testing
        RuntimeScope.install(testScope)
        
        // Reset SignalBus state before each test
        SignalBus.reset()
    }

    @AfterEach
    fun tearDown() {
        RuntimeScope.clear()
        SignalBus.reset()
    }

    @Nested
    @DisplayName("Signal emission")
    inner class SignalEmission {

        @Test
        fun `emits signals to registered reactions`() = runTest(scheduler) {
            val signal = signal<String>("testSignal")
            val value = "test value"
            var reaction1Called = false
            var reaction2Called = false

            val reaction1 = reaction("reaction1", signal) { 
                reaction1Called = true 
            }
            
            val reaction2 = reaction("reaction2", signal) { 
                reaction2Called = true 
            }

            SignalBus.register(signal, reaction1)
            SignalBus.register(signal, reaction2)

            SignalBus.emit(signal, value)
            advanceUntilIdle()

            assertTrue(reaction1Called, "First reaction should be called")
            assertTrue(reaction2Called, "Second reaction should be called")
        }

        @Test
        fun `does not invoke unregistered reactions`() = runTest(scheduler) {
            val signal = signal<String>("testSignal")
            var reactionCalled = false

            val reaction = reaction("testReaction", signal) { 
                reactionCalled = true 
            }

            // Register and then unregister
            SignalBus.register(signal, reaction)
            SignalBus.unregister(signal, reaction)

            SignalBus.emit(signal, "test value")
            advanceUntilIdle()

            assertFalse(reactionCalled, "Unregistered reaction should not be called")
        }

        @Test
        fun `continues processing when reaction throws exception`() = runTest(scheduler) {
            val signal = signal<String>("testSignal")
            var errorReactionCalled = false
            var successReactionCalled = false

            val errorReaction = reaction("errorReaction", signal) {
                errorReactionCalled = true
                throw RuntimeException("Test exception")
            }

            val successReaction = reaction("successReaction", signal) {
                successReactionCalled = true
            }

            SignalBus.register(signal, errorReaction)
            SignalBus.register(signal, successReaction)

            SignalBus.emit(signal, "test value")
            advanceUntilIdle()

            assertTrue(errorReactionCalled, "Error reaction should be called")
            assertTrue(successReactionCalled, "Success reaction should also be called")
        }

        @Test
        fun `only invokes reactions for the emitted signal`() = runTest(scheduler) {
            val signal1 = signal<String>("signal1")
            val signal2 = signal<String>("signal2")
            
            var reaction1Called = false
            var reaction2Called = false

            val reaction1 = reaction("reaction1", signal1) { reaction1Called = true }
            val reaction2 = reaction("reaction2", signal2) { reaction2Called = true }

            SignalBus.register(signal1, reaction1)
            SignalBus.register(signal2, reaction2)

            SignalBus.emit(signal1, "test")
            advanceUntilIdle()

            assertTrue(reaction1Called, "Reaction for emitted signal should be called")
            assertFalse(reaction2Called, "Reaction for non-emitted signal should not be called")
        }

        @Test
        fun `respects type safety of signals and reactions`() = runTest(scheduler) {
            val stringSignal = signal<String>("stringSignal")
            val intSignal = signal<Int>("intSignal")

            var stringReactionCalled = false
            var intReactionCalled = false

            val stringReaction = reaction("stringReaction", stringSignal) { 
                stringReactionCalled = true 
            }
            
            val intReaction = reaction("intReaction", intSignal) { 
                intReactionCalled = true 
            }

            SignalBus.register(stringSignal, stringReaction)
            SignalBus.register(intSignal, intReaction)

            SignalBus.emit(stringSignal, "test")
            advanceUntilIdle()

            assertTrue(stringReactionCalled, "String reaction should be called")
            assertFalse(intReactionCalled, "Int reaction should not be called")
        }
    }

    @Nested
    @DisplayName("Registration management")
    inner class RegistrationManagement {

        @Test
        fun `reset clears all registrations`() = runTest(scheduler) {
            val signal = signal<String>("testSignal")
            var reactionCalled = false

            val reaction = reaction("testReaction", signal) { 
                reactionCalled = true 
            }
            
            SignalBus.register(signal, reaction)

            SignalBus.reset()
            SignalBus.emit(signal, "test")
            advanceUntilIdle()

            assertFalse(reactionCalled, "Reaction should not be called after reset")
        }

        @Test
        fun `registers reaction only once`() = runTest(scheduler) {
            val signal = signal<String>("testSignal")
            var callCount = 0

            val reaction = reaction("testReaction", signal) { 
                callCount++ 
            }

            SignalBus.register(signal, reaction)
            SignalBus.register(signal, reaction)

            SignalBus.emit(signal, "test")
            advanceUntilIdle()

            kotlin.test.assertEquals(1, callCount, "Reaction should only be called once")
        }

        @Test
        fun `safely handles unregistering non-existent reaction`() = runTest(scheduler) {
            val signal = signal<String>("testSignal")
            val reaction = reaction("testReaction", signal) { }

            SignalBus.unregister(signal, reaction)
            
            // No assertions - test passes if no exception is thrown
        }
        
        @Test
        fun `cleans up empty signal registrations`() = runTest(scheduler) {
            // Arrange
            val signal = signal<String>("testSignal")
            val reaction = reaction("testReaction", signal) { }

            SignalBus.register(signal, reaction)
            
            SignalBus.unregister(signal, reaction)
            
            // Create a new reaction and register it to verify the signal entry is recreated
            var called = false
            val newReaction = reaction("newReaction", signal) { 
                called = true 
            }
            
            SignalBus.register(signal, newReaction)

            SignalBus.emit(signal, "test")
            advanceUntilIdle()
            
            assertTrue(called, "New reaction should be called after registration")
        }
    }
}