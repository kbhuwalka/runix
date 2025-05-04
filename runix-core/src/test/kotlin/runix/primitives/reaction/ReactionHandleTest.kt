package runix.primitives.reaction

import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import reaction
import runix.primitives.signal.SignalHandle
import runix.runtime.internal.SignalBus
import kotlin.test.assertEquals
import runix.primitives.module.AppModule

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
        val testModule = createTestModule()

        // Act
        val reactionHandle = reaction("test-reaction", signal, handler)

        // Assert
        // Verify by testing registration behavior since we don't have direct property access
        justRun { SignalBus.register(signal, handler) }
        reactionHandle.register(testModule)
        verify(exactly = 1) { SignalBus.register(signal, handler) }
    }

    @Test
    fun `toString returns formatted string with signal name`() {
        // Arrange
        val signalName = "batteryLow"
        val signal = SignalHandle<Unit>(signalName)
        val handler: suspend (Unit) -> Unit = { /* Do nothing */ }
        val reactionHandle = ReactionHandle("test-reaction", signal, handler)

        // Act
        val result = reactionHandle.toString()

        // Assert
        assertEquals("Reaction(test-reaction)", result)
    }

    @Test
    fun `register forwards handler to SignalBus`() {
        // Arrange
        val signal = SignalHandle<Int>("numericSignal")
        val handler: suspend (Int) -> Unit = { /* Do nothing */ }
        val reactionHandle = ReactionHandle("test-reaction", signal, handler)
        val testModule = createTestModule()

        // Act
        reactionHandle.register(testModule)

        // Assert
        verify(exactly = 1) { SignalBus.register(signal, handler) }
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
    fun `register with Unit signal works correctly`() {
        // Arrange
        val signal = SignalHandle<Unit>("simpleEvent")
        val capturedValues = mutableListOf<Unit>()
        val handler: suspend (Unit) -> Unit = { capturedValues.add(it) }
        val reactionHandle = ReactionHandle("test-reaction", signal, handler)
        val testModule = createTestModule()

        // Act
        reactionHandle.register(testModule)

        // Assert
        verify(exactly = 1) { SignalBus.register(signal, handler) }
    }

    @Test
    fun `register with complex data type works correctly`() {
        // Arrange
        data class ComplexData(val id: String, val value: Int)
        val signal = SignalHandle<ComplexData>("complexEvent")
        val handler: suspend (ComplexData) -> Unit = { /* Do nothing */ }
        val reactionHandle = ReactionHandle("test-reaction", signal, handler)
        val testModule = createTestModule()

        // Act
        reactionHandle.register(testModule)

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

        // Act - simulate the SignalBus calling the handler
        val testMessage = "Hello, world!"
        handlerSlot.captured.invoke(testMessage)

        // Assert
        assertEquals(listOf(testMessage), capturedMessages)
    }

    @Test
    fun `multiple reactions can be registered to the same signal`() {
        // Arrange
        val signal = SignalHandle<Int>("sharedSignal")
        val handler1: suspend (Int) -> Unit = { /* Do nothing */ }
        val handler2: suspend (Int) -> Unit = { /* Do nothing */ }
        val testModule = createTestModule()
        
        val reaction1 = ReactionHandle("test-reaction", signal, handler1)
        val reaction2 = ReactionHandle("test-reaction", signal, handler2)
        
        // Act
        reaction1.register(testModule)
        reaction2.register(testModule)
        
        // Assert
        verify(exactly = 1) { SignalBus.register(signal, handler1) }
        verify(exactly = 1) { SignalBus.register(signal, handler2) }
    }
}