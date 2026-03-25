package runix.primitives.monitor

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import runix.runtime.internal.RuntimeScope
import runix.temporal.time.TestSchedulerTimeProvider
import runix.temporal.time.Time
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class MonitorEvaluationDispatcherTest {

    private val scheduler = TestCoroutineScheduler()
    private val testScope = TestScope(scheduler)
    private val provider = TestSchedulerTimeProvider(scheduler)

    @BeforeEach
    fun setup() {
        Time.setProvider(provider)
        RuntimeScope.install(testScope)
    }

    @AfterEach
    fun tearDown() {
        RuntimeScope.clear()
        Time.resetToRealTime()
    }

    @Test
    fun `requestEvaluate runs evaluation immediately if no active job`() = runTest(scheduler) {
        val evaluationCount = AtomicInteger(0)
        val dispatcher = MonitorEvaluationDispatcher {
            evaluationCount.incrementAndGet()
        }

        dispatcher.requestEvaluate()
        advanceUntilIdle()

        assertEquals(1, evaluationCount.get(), "Evaluation should run immediately")
    }

    @Test
    fun `requestEvaluate coalesces multiple evaluations into one extra run`() = runTest(scheduler) {
        val evaluationCount = AtomicInteger(0)
        val evaluationRequested = AtomicBoolean(false)
        
        // Create a dispatcher with a test hook
        val dispatcher = MonitorEvaluationDispatcher {
            val count = evaluationCount.incrementAndGet()
            if (count == 1) {
                // Signal for our test to make external requests
                evaluationRequested.set(true)
                delay(10) // Delay to simulate work
            }
        }

        // Start the first evaluation
        dispatcher.requestEvaluate()
        
        // Advance a bit to let the first evaluation start
        advanceTimeBy(5)
        assertTrue(evaluationRequested.get(), "First evaluation should be running")
        
        // Make multiple requests while the first evaluation is running
        dispatcher.requestEvaluate()
        dispatcher.requestEvaluate()
        dispatcher.requestEvaluate()
        
        // Complete all evaluations
        advanceUntilIdle()
        
        assertEquals(2, evaluationCount.get(), 
            "Should run once initially and once more after first completes, coalescing multiple requests")
    }

    @Test
    fun `scheduleEvaluateAt runs evaluation at specified time`() = runTest(scheduler) {
        val evaluationCount = AtomicInteger(0)
        val dispatcher = MonitorEvaluationDispatcher {
            evaluationCount.incrementAndGet()
        }

        val targetTime = Time.markNow() + 100.milliseconds
        dispatcher.scheduleEvaluateAt(targetTime)
        
        // Nothing should happen immediately
        assertEquals(0, evaluationCount.get(), "No evaluation should run immediately")
        
        // Advance time to just before the target
        advanceTimeBy(99)
        assertEquals(0, evaluationCount.get(), "No evaluation should run before scheduled time")
        
        // Advance to the scheduled time
        advanceTimeBy(1)
        advanceUntilIdle()
        
        assertEquals(1, evaluationCount.get(), "Evaluation should run at scheduled time")
    }

    @Test
    fun `scheduleEvaluateAt cancels previous scheduled evaluation`() = runTest(scheduler) {
        val evaluationCount = AtomicInteger(0)
        val dispatcher = MonitorEvaluationDispatcher {
            evaluationCount.incrementAndGet()
        }

        // Schedule first evaluation
        dispatcher.scheduleEvaluateAt(Time.markNow() + 100.milliseconds)
        
        // Then schedule another one that will happen later
        dispatcher.scheduleEvaluateAt(Time.markNow() + 200.milliseconds)
        
        // Advance past the first scheduled time
        advanceTimeBy(150)

        // First evaluation should be cancelled
        assertEquals(0, evaluationCount.get(), "First evaluation should be cancelled")
        
        // Advance to the second scheduled time
        advanceTimeBy(50)
        advanceUntilIdle()
        
        assertEquals(1, evaluationCount.get(), "Second evaluation should run")
    }

    @Test
    fun `cancelScheduled cancels upcoming evaluation`() = runTest(scheduler) {
        val evaluationCount = AtomicInteger(0)
        val dispatcher = MonitorEvaluationDispatcher {
            evaluationCount.incrementAndGet()
        }

        // Schedule an evaluation
        dispatcher.scheduleEvaluateAt(Time.markNow() + 100.milliseconds)
        
        // Cancel it before it runs
        dispatcher.cancelScheduled()
        
        // Advance past when it would have run
        advanceTimeBy(150)
        advanceUntilIdle()
        
        assertEquals(0, evaluationCount.get(), "Evaluation should be cancelled")
    }

    @Test
    fun `shutdown cancels both active and scheduled evaluations`() = runTest(scheduler) {
        val completedEvaluations = AtomicInteger(0)
        val runningEvaluation = AtomicInteger(0)
        
        // Create a dispatcher with a slow-running evaluation
        val dispatcher = MonitorEvaluationDispatcher {
            runningEvaluation.incrementAndGet()
            try {
                delay(1000) // Long-running task
                completedEvaluations.incrementAndGet()
            } finally {
                // Finally block will run if cancelled
            }
        }

        // Start an evaluation and schedule another
        dispatcher.requestEvaluate()
        dispatcher.scheduleEvaluateAt(Time.markNow() + 500.milliseconds)
        
        // Give time for evaluation to start
        advanceTimeBy(10)
        assertEquals(1, runningEvaluation.get(), "Evaluation should have started")
        
        // Shut down the dispatcher
        dispatcher.shutdown()
        advanceUntilIdle()
        
        // Advance past when the scheduled job would run
        advanceTimeBy(1000)
        advanceUntilIdle()
        
        assertEquals(0, completedEvaluations.get(), 
            "No evaluations should complete after shutdown")
    }

    @Test
    fun `dispatcher processes new requests after becoming idle`() = runTest(scheduler) {
        val evaluationCount = AtomicInteger(0)
        val dispatcher = MonitorEvaluationDispatcher {
            evaluationCount.incrementAndGet()
        }

        // First batch
        dispatcher.requestEvaluate()
        advanceUntilIdle()
        assertEquals(1, evaluationCount.get(), "First request should run")

        // Second batch after worker has gone idle
        dispatcher.requestEvaluate()
        advanceUntilIdle()
        assertEquals(2, evaluationCount.get(), "Request after idle should restart worker and run")
    }
}