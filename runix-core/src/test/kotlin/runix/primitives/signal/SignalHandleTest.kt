package runix.primitives.signal

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import runix.primitives.signal.SignalBus
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
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
        coJustRun { SignalBus.emit<String>(any(), any()) }

        assertEquals(name, signalHandle.name)
    }

    @Test
    fun `toString returns formatted string with signal name`() {
        val name = "batteryLow"
        val signalHandle = SignalHandle<Unit>(name)

        assertEquals("Signal($name)", signalHandle.toString())
    }

    @Test
    fun `emit forwards call to SignalBus with correct signal and value`() = runTest {
        val name = "dataAvailable"
        val signalHandle = SignalHandle<String>(name)
        val payload = "Test Data"
        coJustRun { SignalBus.emit<String>(any(), any()) }

        signalHandle.emit(payload)

        coVerify(exactly = 1) { SignalBus.emit(signalHandle, payload) }
    }

    @Test
    fun `emit with Unit payload forwards to SignalBus`() = runTest {
        val name = "systemAlert"
        val signalHandle = SignalHandle<Unit>(name)

        signalHandle.emit(Unit)

        coVerify(exactly = 1) { SignalBus.emit(signalHandle, Unit) }
    }

    @Test
    fun `emit with complex data type forwards to SignalBus`() = runTest {
        data class SensorData(val value: Double, val timestamp: Long)
        val name = "sensorReading"
        val signalHandle = SignalHandle<SensorData>(name)
        val payload = SensorData(23.5, System.currentTimeMillis())
        coJustRun { SignalBus.emit<SensorData>(any(), any()) }

        signalHandle.emit(payload)

        coVerify(exactly = 1) { SignalBus.emit(signalHandle, payload) }
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
    fun `signal function creates handle with correct generic type`() = runTest {
        // This test is more about compile-time type checking
        // We're verifying that the types are preserved through the signal() function

        val intSignal = signal<Int>("intEvent")
        val stringSignal = signal<String>("stringEvent")
        val unitSignal = signal<Unit>("unitEvent")

        // Set up SignalBus to return some values so we can test type compatibility
        coEvery { SignalBus.emit(intSignal, any<Int>()) } returns Unit
        coEvery { SignalBus.emit(stringSignal, any<String>()) } returns Unit
        coEvery { SignalBus.emit(unitSignal, Unit) } returns Unit

        // These should compile without errors
        intSignal.emit(42)
        stringSignal.emit("test")
        unitSignal.emit(Unit)

        // Verify the correct types were passed to SignalBus
        coVerify { SignalBus.emit(intSignal, 42) }
        coVerify { SignalBus.emit(stringSignal, "test") }
        coVerify { SignalBus.emit(unitSignal, Unit) }
    }
}
