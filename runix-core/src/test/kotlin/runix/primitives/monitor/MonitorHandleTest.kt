package runix.primitives.monitor

import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import runix.primitives.module.AppModule
import runix.primitives.signal.SignalHandle
import runix.runtime.internal.RuntimeScope
import runix.temporal.FlowBinding
import runix.temporal.MonitoredCondition
import runix.temporal.condition.ConditionEval
import runix.temporal.time.TestSchedulerTimeProvider
import runix.temporal.time.Time
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class MonitorHandleTest {

    // Create test module helper
    private fun createTestModule(name: String = "TestModule"): AppModule {
        return mockk {
            every { this@mockk.name } returns name
        }
    }

    private val scheduler = TestCoroutineScheduler()
    private val testScope = TestScope(scheduler)
    private val provider = TestSchedulerTimeProvider(scheduler)

    @BeforeEach
    fun setup() {
        // Set up time provider for testing
        Time.setProvider(provider)
        
        // Install test scope
        RuntimeScope.install(testScope)
    }

    @AfterEach
    fun tearDown() {
        RuntimeScope.clear()
        Time.resetToRealTime()
        unmockkAll()
    }

    /**
     * A test implementation of MonitoredCondition that produces predictable evaluation results.
     */
    private class TestCondition(
        private val result: () -> ConditionEval = { ConditionEval.True },
        private val onEvaluate: () -> Unit = {}
    ) : MonitoredCondition() {
        override fun build(
            bindings: MutableList<FlowBinding<*>>,
            keyAllocator: KeyAllocator
        ): () -> ConditionEval = {
            onEvaluate()
            result()
        }
    }

    @Test
    fun `monitor registers successfully and prevents double registration`() {
        val testCondition = TestCondition()
        val monitor = MonitorHandle("test", testCondition, null)
        val testModule = createTestModule()

        monitor.register(testModule)
        assertTrue(monitor.isRegistered(), "Monitor should report registered state")

        assertThrows<IllegalStateException> {
            monitor.register(testModule)
        }
    }

    @Test
    fun `monitor start requires registration`() = runTest {
        val testCondition = TestCondition()
        val monitor = MonitorHandle("unregistered", testCondition, null)

        assertThrows<IllegalStateException> {
            monitor.activate()
        }
    }

    @Test
    fun `monitor emits signal when condition evaluates to true`() = runTest {
        val emitted = AtomicBoolean(false)

        val signal = mockk<SignalHandle<Unit>>(relaxed = true)
        every { signal.emit(Unit) } answers { emitted.set(true) }

        val testCondition = TestCondition { ConditionEval.True }
        val monitor = MonitorHandle("test", testCondition, signal)
        val testModule = createTestModule()

        monitor.register(testModule)
        monitor.activate()
        
        // Manually trigger evaluation since we can't rely on dispatcher in test
        monitor.evaluate()
        advanceUntilIdle()

        assertTrue(emitted.get(), "Signal should have been emitted for ConditionEval.True")
        verify { signal.emit(Unit) }
    }

    @Test
    fun `monitor does not emit signal when condition evaluates to false`() = runTest {
        val signal = mockk<SignalHandle<Unit>>(relaxed = true)

        val falseCondition = TestCondition(result = { ConditionEval.False })
        val monitor = MonitorHandle("false-monitor", falseCondition, signal)
        val testModule = createTestModule()

        monitor.register(testModule)
        monitor.activate()
        
        // Manually trigger evaluation
        monitor.evaluate()
        advanceUntilIdle()

        verify(exactly = 0) { signal.emit(Unit) }
    }

    @Test
    fun `monitor schedules delayed evaluation`() = runTest {
        val delayTime = 200.milliseconds
        val delayMark = Time.markNow() + delayTime
        val delayedCondition = TestCondition(result = { ConditionEval.Delayed(delayMark) })

        // Create a monitor with a spy dispatcher to verify method calls
        val dispatcher = spyk(MonitorEvaluationDispatcher { })
        val monitor = MonitorHandle("delayed-monitor", delayedCondition, null) 
        
        // Replace the monitor's dispatcher with our spy
        val field = MonitorHandle::class.java.getDeclaredField("dispatcher")
        field.isAccessible = true
        field.set(monitor, dispatcher)
        
        val testModule = createTestModule()
        monitor.register(testModule)
        monitor.activate()
        
        // Manually trigger evaluation
        monitor.evaluate()
        advanceUntilIdle()

        // Verify the dispatcher was told to schedule the evaluation
        coVerify { dispatcher.scheduleEvaluateAt(eq(delayMark)) }
    }

    @Test
    fun `monitor stop shuts down the dispatcher`() = runTest {
        val testCondition = TestCondition()
        
        // Create a monitor with a spy dispatcher to verify method calls
        val dispatcher = spyk(MonitorEvaluationDispatcher { })
        val monitor = MonitorHandle("stop-test", testCondition, null)
        
        // Replace the monitor's dispatcher with our spy
        val field = MonitorHandle::class.java.getDeclaredField("dispatcher")
        field.isAccessible = true
        field.set(monitor, dispatcher)
        
        val testModule = createTestModule()
        monitor.register(testModule)
        monitor.activate()
        
        // Clear any verification history
        clearMocks(dispatcher, answers = false)
        
        // Stop the monitor
        monitor.deactivate()
        
        // Verify the dispatcher was shut down
        coVerify { dispatcher.shutdown() }
    }

    @Test
    fun `monitor can safely stop without starting`() = runTest {
        val testCondition = TestCondition()
        val monitor = MonitorHandle("untouched", testCondition, null)

        // Should not throw exceptions
        monitor.deactivate()
    }

    @Test
    fun `monitor correctly starts evaluation pipeline`() = runTest {
        // Create a test condition that tracks evaluation
        val evalCount = AtomicInteger(0)
        val testCondition = TestCondition(
            onEvaluate = { evalCount.incrementAndGet() },
            result = { ConditionEval.True }
        )

        // Create the monitor with the test condition
        val monitor = MonitorHandle("evaluation-test", testCondition, null)
        val testModule = createTestModule()

        // Register and activate
        monitor.register(testModule)
        monitor.activate()

        // Manually evaluate to exercise the pipeline
        monitor.evaluate()
        advanceUntilIdle()

        // Verify that evaluation happened
        assertTrue(evalCount.get() > 0,
            "Monitor should evaluate the condition when running")
    }

    @Test
    fun `monitor does not re-evaluate if already started`() = runTest {
        val startCount = AtomicInteger(0)
        val testCondition = TestCondition()
        val monitor = MonitorHandle("single-eval", testCondition, null)
        val testModule = createTestModule()

        // Mock the CompiledMonitor to count start calls
        val compiledField = MonitorHandle::class.java.getDeclaredField("compiled")
        compiledField.isAccessible = true
        val mockCompiled = mockk<runix.temporal.CompiledMonitor>()
        coEvery { mockCompiled.start(any()) } coAnswers { 
            startCount.incrementAndGet()
        }
        
        monitor.register(testModule)
        compiledField.set(monitor, mockCompiled)
        
        // Start once
        monitor.activate()
        advanceUntilIdle()
        
        // Try to start again
        monitor.activate()
        advanceUntilIdle()

        assertEquals(
            1,
            startCount.get(),
            "Monitor should only start the compiled monitor once"
        )
    }

    @Test
    fun `evaluate handles all condition evaluation types`() = runTest {
        val signal = mockk<SignalHandle<Unit>>(relaxed = true)
        val evalResults = mutableListOf<ConditionEval>(
            ConditionEval.True,
            ConditionEval.False,
            ConditionEval.Delayed(Time.markNow() + 100.milliseconds)
        )
        
        // Dispatcher spy to verify interactions
        val dispatcher = spyk(MonitorEvaluationDispatcher { })
        
        // Use a condition that returns different results on subsequent calls
        var evalIndex = 0
        val testCondition = TestCondition(
            result = { 
                evalResults[evalIndex].also { evalIndex = (evalIndex + 1) % evalResults.size }
            }
        )
        
        val monitor = MonitorHandle("multi-eval", testCondition, signal)
        
        // Replace the monitor's dispatcher with our spy
        val dispatcherField = MonitorHandle::class.java.getDeclaredField("dispatcher")
        dispatcherField.isAccessible = true
        dispatcherField.set(monitor, dispatcher)
        
        val testModule = createTestModule()
        monitor.register(testModule)
        
        // First evaluation - True
        monitor.evaluate()
        verify(exactly = 1) { signal.emit(Unit) }
        coVerify(exactly = 1) { dispatcher.cancelScheduled() }
        coVerify(exactly = 0) { dispatcher.scheduleEvaluateAt(any()) }
        
        // Second evaluation - False  
        monitor.evaluate()
        verify(exactly = 1) { signal.emit(Unit) } // Still only once
        coVerify(exactly = 2) { dispatcher.cancelScheduled() }
        coVerify(exactly = 0) { dispatcher.scheduleEvaluateAt(any()) }
        
        // Third evaluation - Delayed
        val delayedResult = evalResults[2] as ConditionEval.Delayed
        monitor.evaluate()
        verify(exactly = 1) { signal.emit(Unit) } // Still only once
        coVerify(exactly = 3) { dispatcher.cancelScheduled() }
        coVerify(exactly = 1) { dispatcher.scheduleEvaluateAt(eq(delayedResult.nextCheckAt)) }
    }
}