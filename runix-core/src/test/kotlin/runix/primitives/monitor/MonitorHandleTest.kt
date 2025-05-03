package runix.primitives.monitor

import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import runix.primitives.signal.SignalHandle
import runix.runtime.RuntimeScheduler
import runix.temporal.FlowBinding
import runix.temporal.MonitoredCondition
import runix.temporal.condition.ConditionEval
import runix.temporal.time.TestSchedulerTimeProvider
import runix.temporal.time.Time
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class MonitorHandleTest {

    private val scheduler = TestCoroutineScheduler()
    private val testScope = TestScope(scheduler)
    private val provider = TestSchedulerTimeProvider(scheduler)
    
    // Mock the RuntimeScheduler for testing
    private val mockRuntimeScheduler = mockk<RuntimeScheduler>(relaxed = true)
    
    private val recheckJobSlot = slot<suspend () -> Unit>()
    
    @BeforeTest
    fun setup() {
        Time.setProvider(provider)
        
        // Clear the slot for each test
        if (recheckJobSlot.isCaptured) {
            recheckJobSlot.clear()
        }
        
        // Mock the static scheduleRecheck and cancelRecheck methods
        mockkObject(RuntimeScheduler)
        
        // Configure the scheduleRecheck mock to capture the job lambda
        every { 
            RuntimeScheduler.scheduleRecheck(any(), any(), capture(recheckJobSlot))
        } returns Unit
        
        justRun { RuntimeScheduler.cancelRecheck(any()) }
    }
    
    @AfterTest
    fun tearDown() {
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

        monitor.register("test-module")
        assertTrue(monitor.isRegistered(), "Monitor should report registered state")

        assertFailsWith<IllegalStateException> {
            monitor.register("test-module")
        }
    }

    @Test
    fun `monitor start requires registration`() {
        val testCondition = TestCondition()
        val monitor = MonitorHandle("unregistered", testCondition, null)

        assertFailsWith<IllegalStateException> {
            monitor.start()
        }
    }

    @Test
    fun `monitor emits signal when condition evaluates to true`() = testScope.runTest {
        val emitted = AtomicBoolean(false)
        
        val signal = mockk<SignalHandle<Unit>>(relaxed = true)
        every { signal.emit(Unit) } answers { emitted.set(true) }
        
        val testCondition = TestCondition { ConditionEval.True }
        val monitor = MonitorHandle("test", testCondition, signal)

        monitor.register("test-module")
        monitor.start()
        advanceUntilIdle()
        
        assertTrue(emitted.get(), "Signal should have been emitted for ConditionEval.True")
        verify { signal.emit(Unit) }
    }

    @Test
    fun `monitor does not emit signal when condition evaluates to false`() = testScope.runTest {
        val signal = mockk<SignalHandle<Unit>>(relaxed = true)
        
        val falseCondition = TestCondition(result = { ConditionEval.False })
        val monitor = MonitorHandle("false-monitor", falseCondition, signal)

        monitor.register("test-module")
        monitor.start()
        advanceUntilIdle()
        
        verify(exactly = 0) { signal.emit(Unit) }
    }

    @Test
    fun `monitor reschedules on delayed evaluation`() = testScope.runTest {
        val delayTime = 200.milliseconds
        val delayMark = Time.markNow() + delayTime
        val delayedCondition = TestCondition(result = { ConditionEval.Delayed(delayMark) } )

        val monitor = MonitorHandle("delayed-monitor", delayedCondition, null)

        monitor.register("test-module")
        monitor.start()
        advanceUntilIdle()
        
        verify { 
            RuntimeScheduler.scheduleRecheck(
                monitor = eq(monitor),
                mark = eq(delayMark),
                job = any()
            ) 
        }
    }

    @Test
    fun `monitor stop cancels future rechecks`() = testScope.runTest {
        val delayedCondition = TestCondition(result = { ConditionEval.Delayed(Time.markNow() + 10.seconds) })
        val monitor = MonitorHandle("stop-test", delayedCondition, null)

        monitor.register("test-module")
        monitor.start()
        advanceUntilIdle()
        
        // Clear verification history
        clearMocks(RuntimeScheduler)
        
        monitor.stop()
        
        verify { RuntimeScheduler.cancelRecheck(eq(monitor)) }
    }

    @Test
    fun `monitor can safely stop without starting`() {
        val testCondition = TestCondition()
        val monitor = MonitorHandle("untouched", testCondition, null)

        // Should not throw exceptions
        monitor.stop()
    }

    @Test
    fun `monitor correctly starts evaluation pipeline`() = testScope.runTest {
        val monitorLog = mutableListOf<String>()
        val testCondition = TestCondition(
            onEvaluate = { monitorLog += "evaluated" }
        )

        val monitor = MonitorHandle("evaluation-test", testCondition, null)

        monitor.register("test-module")
        monitor.start()
        advanceUntilIdle()

        assertTrue(
            monitorLog.contains("evaluated"),
            "Monitor should evaluate its condition when started"
        )
    }

    @Test
    fun `monitor does not re-evaluate if already started`() = testScope.runTest {
        val evalCount = AtomicInteger(0)
        val testCondition = TestCondition(
            onEvaluate = { evalCount.incrementAndGet() }
        )

        val monitor = MonitorHandle("single-eval", testCondition, null)

        monitor.register("test-module")
        monitor.start()
        advanceUntilIdle()
        
        // Try to start again
        monitor.start()
        advanceUntilIdle()

        assertEquals(
            1,
            evalCount.get(),
            "Monitor should only evaluate condition once after start"
        )
    }
    
    @Test
    fun `delayed evaluation can be triggered multiple times`() = testScope.runTest {
        val evaluations = AtomicInteger(0)
        val testCondition = TestCondition(
            result = { 
                val count = evaluations.incrementAndGet()
                if (count < 3) {
                    ConditionEval.Delayed(Time.markNow() + 100.milliseconds)
                } else {
                    ConditionEval.True
                }
            }
        )
        
        val monitor = MonitorHandle("time-based", testCondition, null)
        monitor.register("test-module")
        monitor.start()
        advanceUntilIdle()
        
        // First evaluation happens on start
        assertEquals(1, evaluations.get(), "First evaluation should happen on start")
        verify { RuntimeScheduler.scheduleRecheck(eq(monitor), any(), any()) }
        
        // Execute the captured recheck job to simulate scheduler triggering it
        assertTrue(recheckJobSlot.isCaptured, "Recheck job should be captured")
        recheckJobSlot.captured.invoke()
        advanceUntilIdle()
        
        // Second evaluation
        assertEquals(2, evaluations.get(), "Second evaluation should happen on first recheck")
        verify(exactly = 2) { RuntimeScheduler.scheduleRecheck(eq(monitor), any(), any()) }
        
        // Execute the recheck job again
        recheckJobSlot.captured.invoke()
        advanceUntilIdle()
        
        // Third evaluation returns True, should not schedule another recheck
        assertEquals(3, evaluations.get(), "Third evaluation should happen on second recheck")
        verify(exactly = 2) { RuntimeScheduler.scheduleRecheck(eq(monitor), any(), any()) }
    }
}