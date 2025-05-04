package runix.primitives.reaction

import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import reaction
import runix.primitives.module.AppModule
import runix.primitives.signal.SignalHandle
import runix.runtime.internal.SignalBus
import kotlin.test.assertEquals

class ReactionHandleTest {

    // Add a helper method to create test modules
    private fun createTestModule(name: String = "TestModule"): AppModule {
        return mockk {
            every { this@mockk.name } returns name
        }
    }

    @BeforeEach
    fun setup() {
        mockkObject(SignalBus)
        justRun { SignalBus.register<Any?>(any(), any()) }
        justRun { SignalBus.unregister<Any?>(any(), any()) }
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SignalBus)
    }

    @Test
    fun `reaction function creates ReactionHandle with correct signal and handler`() {
        // Arrange
        val signalName = "testSignal"
        val signal = SignalHandle<String>(signalName)
        val handler: suspend (String) -> Unit = { /* Do nothing */ }

        // Act
        val reactionHandle = reaction("test-reaction", signal, handler)

        // Assert
        assertEquals("test-reaction", reactionHandle.name)
        // Indirectly test the private fields via the activate method
        val testModule = createTestModule()
        reactionHandle.register(testModule)
        reactionHandle.activate()
        verify(exactly = 1) { SignalBus.register(signal, handler) }
    }

    @Test
    fun `toString returns formatted string with reaction name`() {
        // Arrange
        val signal = SignalHandle<Unit>("batteryLow")
        val handler: suspend (Unit) -> Unit = { /* Do nothing */ }
        val reactionHandle = ReactionHandle("test-reaction", signal, handler)

        // Act
        val result = reactionHandle.toString()

        // Assert
        assertEquals("Reaction(test-reaction)", result)
    }

    @Test
    fun `register only establishes ownership without activating`() {
        // Arrange
        val signal = SignalHandle<Int>("numericSignal")
        val handler: suspend (Int) -> Unit = { /* Do nothing */ }
        val reactionHandle = ReactionHandle("test-reaction", signal, handler)
        val testModule = createTestModule()

        // Act
        reactionHandle.register(testModule)

        // Assert - should not have registered with SignalBus yet
        verify(exactly = 0) { SignalBus.register(any<SignalHandle<Int>>(), any()) }
    }

    @Test
    fun `activate registers handler with SignalBus`() {
        // Arrange
        val signal = SignalHandle<Int>("numericSignal")
        val handler: suspend (Int) -> Unit = { /* Do nothing */ }
        val reactionHandle = ReactionHandle("test-reaction", signal, handler)
        val testModule = createTestModule()

        // Act
        reactionHandle.register(testModule)
        reactionHandle.activate()

        // Assert
        verify(exactly = 1) { SignalBus.register(signal, handler) }
    }

    @Test
    fun `deactivate unregisters handler from SignalBus`() {
        // Arrange
        val signal = SignalHandle<Int>("numericSignal")
        val handler: suspend (Int) -> Unit = { /* Do nothing */ }
        val reactionHandle = ReactionHandle("test-reaction", signal, handler)
        val testModule = createTestModule()

        // Act
        reactionHandle.register(testModule)
        reactionHandle.activate()
        reactionHandle.deactivate()

        // Assert
        verify(exactly = 1) { SignalBus.unregister(signal, handler) }
    }

    @Test
    fun `activate without registration throws exception`() {
        // Arrange
        val signal = SignalHandle<Boolean>("flagSignal")
        val handler: suspend (Boolean) -> Unit = { /* Do nothing */ }
        val reactionHandle = ReactionHandle("test-reaction", signal, handler)

        // Act & Assert
        assertThrows<IllegalStateException> {
            reactionHandle.activate()
        }
    }

    @Test
    fun `register throws when called twice`() {
        // Arrange
        val signal = SignalHandle<Boolean>("flagSignal")
        val handler: suspend (Boolean) -> Unit = { /* Do nothing */ }
        val reactionHandle = ReactionHandle("test-reaction", signal, handler)
        val testModule = createTestModule()

        // Register once successfully
        reactionHandle.register(testModule)

        // Act & Assert
        assertThrows<IllegalStateException> {
            reactionHandle.register(testModule)
        }
    }

    @Test
    fun `register with different module names still throws on second call`() {
        // Arrange
        val signal = SignalHandle<Double>("sensorSignal")
        val handler: suspend (Double) -> Unit = { /* Do nothing */ }
        val reactionHandle = ReactionHandle("test-reaction", signal, handler)
        val testModule1 = createTestModule("TestModule1")
        val testModule2 = createTestModule("TestModule2")

        // Register once successfully
        reactionHandle.register(testModule1)

        // Act & Assert
        assertThrows<IllegalStateException> {
            reactionHandle.register(testModule2)
        }
    }

    @Test
    fun `multiple activate calls only register once`() {
        // Arrange
        val signal = SignalHandle<String>("repeatedActivation")
        val handler: suspend (String) -> Unit = { /* Do nothing */ }
        val reactionHandle = ReactionHandle("test-reaction", signal, handler)
        val testModule = createTestModule()

        // Act
        reactionHandle.register(testModule)
        reactionHandle.activate()
        reactionHandle.activate() // Second call should be ignored
        reactionHandle.activate() // Third call should be ignored

        // Assert
        verify(exactly = 1) { SignalBus.register(signal, handler) }
    }

    @Test
    fun `deactivate without activation does nothing`() {
        // Arrange
        val signal = SignalHandle<String>("noActivation")
        val handler: suspend (String) -> Unit = { /* Do nothing */ }
        val reactionHandle = ReactionHandle("test-reaction", signal, handler)
        val testModule = createTestModule()

        // Act
        reactionHandle.register(testModule)
        reactionHandle.deactivate() // Should do nothing since not activated

        // Assert
        verify(exactly = 0) { SignalBus.unregister(any<SignalHandle<String>>(), any()) }
    }

    @Test
    fun `register with Unit signal works correctly with activation`() {
        // Arrange
        val signal = SignalHandle<Unit>("simpleEvent")
        val capturedValues = mutableListOf<Unit>()
        val handler: suspend (Unit) -> Unit = { capturedValues.add(it) }
        val reactionHandle = ReactionHandle("test-reaction", signal, handler)
        val testModule = createTestModule()

        // Act
        reactionHandle.register(testModule)
        reactionHandle.activate()

        // Assert
        verify(exactly = 1) { SignalBus.register(signal, handler) }
    }

    @Test
    fun `handler function receives correct payload through SignalBus registration`() = runTest {
        // Arrange
        val signal = SignalHandle<String>("messageSignal")
        val capturedMessages = mutableListOf<String>()
        val handler: suspend (String) -> Unit = { capturedMessages.add(it) }
        val testModule = createTestModule()

        // Capture and execute the handler when SignalBus.register is called
        val handlerSlot = slot<suspend (String) -> Unit>()
        justRun { SignalBus.register(eq(signal), capture(handlerSlot)) }

        val reactionHandle = ReactionHandle("test-reaction", signal, handler)
        reactionHandle.register(testModule)
        reactionHandle.activate()

        // Act - simulate the SignalBus calling the handler
        val testMessage = "Hello, world!"
        handlerSlot.captured.invoke(testMessage)

        // Assert
        assertEquals(listOf(testMessage), capturedMessages)
    }

    @Test
    fun `multiple reactions can be registered and activated for the same signal`() {
        // Arrange
        val signal = SignalHandle<Int>("sharedSignal")
        val handler1: suspend (Int) -> Unit = { /* Do nothing */ }
        val handler2: suspend (Int) -> Unit = { /* Do nothing */ }
        val testModule = createTestModule()

        val reaction1 = ReactionHandle("reaction-1", signal, handler1)
        val reaction2 = ReactionHandle("reaction-2", signal, handler2)

        // Act
        reaction1.register(testModule)
        reaction2.register(testModule)
        reaction1.activate()
        reaction2.activate()

        // Assert
        verify(exactly = 1) { SignalBus.register(signal, handler1) }
        verify(exactly = 1) { SignalBus.register(signal, handler2) }
    }

    @Test
    fun `lifecycle of activation and deactivation works correctly`() {
        // Arrange
        val signal = SignalHandle<String>("lifecycleSignal")
        val handler: suspend (String) -> Unit = { /* Do nothing */ }
        val reactionHandle = ReactionHandle("test-reaction", signal, handler)
        val testModule = createTestModule()

        // Act & Assert - Full lifecycle
        reactionHandle.register(testModule)

        // First activation
        reactionHandle.activate()
        verify(exactly = 1) { SignalBus.register(signal, handler) }

        // Deactivation
        reactionHandle.deactivate()
        verify(exactly = 1) { SignalBus.unregister(signal, handler) }

        // Second activation
        reactionHandle.activate()
        verify(exactly = 2) { SignalBus.register(signal, handler) }

        // Second deactivation
        reactionHandle.deactivate()
        verify(exactly = 2) { SignalBus.unregister(signal, handler) }
    }
}