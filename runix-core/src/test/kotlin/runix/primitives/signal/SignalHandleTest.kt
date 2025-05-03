package runix.primitives.signal

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import runix.runtime.internal.SignalBus
import kotlin.test.assertEquals

class SignalHandleTest {

    @BeforeEach
    fun setup() {
        mockkObject(SignalBus)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SignalBus)
    }

    @Test
    fun `signal function creates handle with correct name`() {
        val name = "testSignal"
        val signalHandle = signal<String>(name)
        justRun { SignalBus.emit<String>(any(), any()) }

        assertEquals(name, signalHandle.name)
    }

    @Test
    fun `toString returns formatted string with signal name`() {
        val name = "batteryLow"
        val signalHandle = SignalHandle<Unit>(name)

        assertEquals("Signal($name)", signalHandle.toString())
    }

    @Test
    fun `emit forwards call to SignalBus with correct signal and value`() {
        // Arrange
        val name = "dataAvailable"
        val signalHandle = SignalHandle<String>(name)
        val payload = "Test Data"
        justRun { SignalBus.emit<String>(any(), any()) }


        // Act
        signalHandle.emit(payload)
        
        // Assert
        verify(exactly = 1) { SignalBus.emit(signalHandle, payload) }
    }
    
    @Test
    fun `emit with Unit payload forwards to SignalBus`() {
        // Arrange
        val name = "systemAlert"
        val signalHandle = SignalHandle<Unit>(name)

        // Act
        signalHandle.emit(Unit)
        
        // Assert
        verify(exactly = 1) { SignalBus.emit(signalHandle, Unit) }
    }
    
    @Test
    fun `emit with complex data type forwards to SignalBus`() {
        // Arrange
        data class SensorData(val value: Double, val timestamp: Long)
        val name = "sensorReading"
        val signalHandle = SignalHandle<SensorData>(name)
        val payload = SensorData(23.5, System.currentTimeMillis())
        justRun { SignalBus.emit<SensorData>(any(), any()) }


        // Act
        signalHandle.emit(payload)
        
        // Assert
        verify(exactly = 1) { SignalBus.emit(signalHandle, payload) }
    }
    
    @Test
    fun `multiple signals with same name are distinct objects`() {
        // Arrange
        val name = "commonEvent"
        val signal1 = signal<String>(name)
        val signal2 = signal<String>(name)
        
        // No equality override, so references should be different
        assert(signal1 !== signal2) { "Expected distinct signal instances" }
    }
    
    @Test 
    fun `signal function creates handle with correct generic type`() {
        // This test is more about compile-time type checking
        // We're verifying that the types are preserved through the signal() function
        
        val intSignal = signal<Int>("intEvent")
        val stringSignal = signal<String>("stringEvent")
        val unitSignal = signal<Unit>("unitEvent")
        
        // Set up SignalBus to return some values so we can test type compatibility
        every { SignalBus.emit(intSignal, any<Int>()) } returns Unit
        every { SignalBus.emit(stringSignal, any<String>()) } returns Unit
        every { SignalBus.emit(unitSignal, Unit) } returns Unit
        
        // These should compile without errors
        intSignal.emit(42)
        stringSignal.emit("test")
        unitSignal.emit(Unit)
        
        // Verify the correct types were passed to SignalBus
        verify { SignalBus.emit(intSignal, 42) }
        verify { SignalBus.emit(stringSignal, "test") }
        verify { SignalBus.emit(unitSignal, Unit) }
    }
}