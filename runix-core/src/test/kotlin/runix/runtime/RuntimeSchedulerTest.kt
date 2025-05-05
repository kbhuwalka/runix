package runix.runtime

import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import runix.primitives.monitor.MonitorHandle
import runix.runtime.internal.RuntimeScope
import runix.temporal.MonitoredCondition
import runix.temporal.time.TestSchedulerTimeProvider
import runix.temporal.time.Time
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("RuntimeScheduler")
class RuntimeSchedulerTest {

    // Test infrastructure setup
    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)
    private val testScope = TestScope(testDispatcher)
    private val timeProvider = TestSchedulerTimeProvider(testScheduler)
    
    // Test monitors as simple dummy objects
    private lateinit var testMonitor1: MonitorHandle
    private lateinit var testMonitor2: MonitorHandle

    @BeforeEach
    fun setup() {
        // Setup RuntimeScope mocking with the helper
        RuntimeScope.install(testScope)
        
        // Setup Time provider for test scheduler
        Time.setProvider(timeProvider)
        
        // Reset scheduler state
        RuntimeScheduler.stop()
        
        // Create simple monitor objects - we're just using them as keys
        testMonitor1 = createTestMonitor("TestMonitor1")
        testMonitor2 = createTestMonitor("TestMonitor2")
    }

    @AfterEach
    fun tearDown() {
        RuntimeScope.clear()
        Time.resetToRealTime()
        RuntimeScheduler.stop()
        unmockkAll()
    }
    
    // Helper to create a minimal MonitorHandle implementation for testing
    private fun createTestMonitor(name: String): MonitorHandle {
        // Create a minimal dummy MonitoredCondition
        val dummyCondition = mockk<MonitoredCondition>(relaxed = true)
        // Create a MonitorHandle with the given name
        return MonitorHandle(name, dummyCondition, null)
    }

    @Nested
    @DisplayName("Starting and stopping")
    inner class StartingAndStopping {

        @Test
        @DisplayName("should start successfully when not already running")
        fun startsSuccessfullyWhenNotRunning() {
            // Act
            RuntimeScheduler.start()
            
            // No assertions needed - test passes if no exception is thrown
        }

        @Test
        @DisplayName("should throw when starting while already running")
        fun throwsWhenStartingWhileRunning() {
            // Arrange
            RuntimeScheduler.start()
            
            // Act & Assert
            var exceptionThrown = false
            try {
                RuntimeScheduler.start()
            } catch (e: IllegalStateException) {
                exceptionThrown = true
            }
            
            assertTrue(exceptionThrown, "Should throw IllegalStateException when starting while already running")
        }

        @Test
        @DisplayName("should stop successfully when running")
        fun stopsSuccessfullyWhenRunning() {
            // Arrange
            RuntimeScheduler.start()
            
            // Act
            RuntimeScheduler.stop()
            
            // Assert - should be able to start again
            RuntimeScheduler.start() // This would throw if stop didn't work
        }

        @Test
        @DisplayName("should do nothing when stopping while not running")
        fun doesNothingWhenStoppingWhileNotRunning() {
            // Act
            RuntimeScheduler.stop()
            
            // Assert - should be able to start
            RuntimeScheduler.start() // This would throw if already running
        }
    }

    @Nested
    @DisplayName("Scheduling rechecks")
    inner class SchedulingRechecks {

        @Test
        @DisplayName("should execute job when timestamp is reached")
        fun executesJobWhenTimestampReached() = runTest(testScheduler) {
            // Arrange
            RuntimeScheduler.start()
            val executed = AtomicBoolean(false)
            
            // Act
            val now = Time.markNow()
            val futureTime = now.plus(5.seconds)
            
            RuntimeScheduler.scheduleRecheck(testMonitor1, futureTime) {
                executed.set(true)
            }
            
            // Assert - not executed immediately
            assertFalse(executed.get(), "Should not execute job immediately")
            
            // Advance time and check again
            advanceTimeBy(5.seconds)
            advanceUntilIdle()
            
            assertTrue(executed.get(), "Should execute job after time advances past mark")
        }

        @Test
        @DisplayName("should cancel previous job when scheduling a new one for the same monitor")
        fun cancelsPreviousJobWhenSchedulingNewOne() = runTest(testScheduler) {
            // Arrange
            RuntimeScheduler.start()
            val firstJobExecuted = AtomicBoolean(false)
            val secondJobExecuted = AtomicBoolean(false)
            
            // Schedule first job
            val now = Time.markNow()
            val firstFutureTime = now.plus(10.seconds)
            
            RuntimeScheduler.scheduleRecheck(testMonitor1, firstFutureTime) {
                firstJobExecuted.set(true)
            }
            
            // Schedule second job for same monitor
            val secondFutureTime = now.plus(5.seconds)
            
            RuntimeScheduler.scheduleRecheck(testMonitor1, secondFutureTime) {
                secondJobExecuted.set(true)
            }
            
            // Advance time past second job
            advanceTimeBy(6.seconds)
            advanceUntilIdle()
            
            // Assert - second job executed, first job not
            assertTrue(secondJobExecuted.get(), "Second job should be executed")
            assertFalse(firstJobExecuted.get(), "First job should be cancelled")
            
            // Advance time past first job
            advanceTimeBy(5.seconds)
            advanceUntilIdle()
            
            // First job still not executed
            assertFalse(firstJobExecuted.get(), "First job should remain cancelled")
        }

        @Test
        @DisplayName("should handle multiple monitors independently")
        fun handlesMultipleMonitorsIndependently() = runTest(testScheduler) {
            // Arrange
            RuntimeScheduler.start()
            val monitor1JobExecuted = AtomicBoolean(false)
            val monitor2JobExecuted = AtomicBoolean(false)

            val now = Time.markNow()

            // Schedule job for first monitor
            RuntimeScheduler.scheduleRecheck(testMonitor1, now.plus(3.seconds)) {
                monitor1JobExecuted.set(true)
            }

            // Schedule job for second monitor
            RuntimeScheduler.scheduleRecheck(testMonitor2, now.plus(7.seconds)) {
                monitor2JobExecuted.set(true)
            }

            // Advance time past first job, but process ONLY tasks up to 4 seconds
            testScheduler.advanceTimeBy(4.seconds) // Use testScheduler directly for more control

            // Assert - first job executed, second job not
            assertTrue(monitor1JobExecuted.get(), "First monitor's job should be executed")
            assertFalse(monitor2JobExecuted.get(), "Second monitor's job should not be executed yet")

            // Advance time past second job
            testScheduler.advanceTimeBy(3.seconds) // From 4s to 7s
            advanceUntilIdle()

            // Assert - both jobs executed
            assertTrue(monitor2JobExecuted.get(), "Second monitor's job should be executed")
        }
    }

    @Nested
    @DisplayName("Cancelling rechecks")
    inner class CancellingRechecks {

        @Test
        @DisplayName("should cancel a scheduled recheck")
        fun cancelsScheduledRecheck() = runTest(testScheduler) {
            // Arrange
            RuntimeScheduler.start()
            val jobExecuted = AtomicBoolean(false)
            
            val now = Time.markNow()
            val futureTime = now.plus(5.seconds)

            // Schedule a job
            RuntimeScheduler.scheduleRecheck(testMonitor1, futureTime) {
                jobExecuted.set(true)
            }
            
            // Cancel the job
            RuntimeScheduler.cancelRecheck(testMonitor1)
            
            // Advance time past when job would execute
            advanceTimeBy(6.seconds)
            advanceUntilIdle()
            
            // Assert
            assertFalse(jobExecuted.get(), "Job should be cancelled and not execute")
        }

        @Test
        @DisplayName("should do nothing when cancelling a non-existent job")
        fun doesNothingWhenCancellingNonExistentJob() = runTest(testScheduler) {
            // Arrange
            RuntimeScheduler.start()
            
            // Act - should not throw
            RuntimeScheduler.cancelRecheck(testMonitor1)
            
            // No assertions needed - test passes if no exception is thrown
        }

        @Test
        @DisplayName("should only cancel the specified monitor's job")
        fun onlyCancelsSpecifiedMonitorsJob() = runTest(testScheduler) {

            println("Scope is test scope: ${RuntimeScope.scope === this}")  // should be true

            // Arrange
            RuntimeScheduler.start()
            val monitor1JobExecuted = AtomicBoolean(false)
            val monitor2JobExecuted = AtomicBoolean(false)
            
            val now = Time.markNow()

            // Schedule jobs for both monitors
            RuntimeScheduler.scheduleRecheck(testMonitor1, now.plus(5.seconds)) {
                monitor1JobExecuted.set(true)
            }
            
            RuntimeScheduler.scheduleRecheck(testMonitor2, now.plus(5.seconds)) {
                monitor2JobExecuted.set(true)
            }


            // Cancel only first monitor's job
            RuntimeScheduler.cancelRecheck(testMonitor1)

            // Advance time
            advanceTimeBy(6.seconds)
            advanceUntilIdle()

            // Assert
            assertFalse(monitor1JobExecuted.get(), "First monitor's job should be cancelled")
            assertTrue(monitor2JobExecuted.get(), "Second monitor's job should still execute")
        }
    }

    @Nested
    @DisplayName("Stopping behavior")
    inner class StoppingBehavior {

        @Test
        @DisplayName("should cancel all scheduled jobs when stopping")
        fun cancelsAllJobsWhenStopping() = runTest(testScheduler) {
            // Arrange
            RuntimeScheduler.start()
            val jobExecutionCount = AtomicInteger(0)
            
            val now = Time.markNow()
            
            // Schedule multiple jobs
            RuntimeScheduler.scheduleRecheck(testMonitor1, now.plus(5.seconds)) {
                jobExecutionCount.incrementAndGet()
            }
            
            RuntimeScheduler.scheduleRecheck(testMonitor2, now.plus(8.seconds)) {
                jobExecutionCount.incrementAndGet()
            }
            
            // Stop the scheduler
            RuntimeScheduler.stop()
            
            // Advance time past all jobs
            advanceTimeBy(10.seconds)
            advanceUntilIdle()
            
            // Assert
            assertEquals(0, jobExecutionCount.get(), "No jobs should execute after stopping")
        }

        @Test
        @DisplayName("should allow new jobs to be scheduled after restart")
        fun allowsNewJobsAfterRestart() = runTest(testScheduler) {
            // Arrange
            RuntimeScheduler.start()
            val beforeStopExecuted = AtomicBoolean(false)
            val afterRestartExecuted = AtomicBoolean(false)
            
            val now = Time.markNow()
            
            // Schedule a job
            RuntimeScheduler.scheduleRecheck(testMonitor1, now.plus(5.seconds)) {
                beforeStopExecuted.set(true)
            }
            
            // Stop and restart
            RuntimeScheduler.stop()
            RuntimeScheduler.start()
            advanceUntilIdle()
            
            // Schedule a new job
            RuntimeScheduler.scheduleRecheck(testMonitor1, now.plus(3.seconds)) {
                afterRestartExecuted.set(true)
            }
            
            // Advance time
            advanceTimeBy(4.seconds)
            advanceUntilIdle()
            
            // Assert
            assertFalse(beforeStopExecuted.get(), "Job scheduled before stop should not execute")
            assertTrue(afterRestartExecuted.get(), "Job scheduled after restart should execute")
        }
    }

    @Nested
    @DisplayName("Exception handling")
    inner class ExceptionHandling {

        @Test
        @DisplayName("should handle exceptions in scheduled jobs")
        fun handlesExceptionsInScheduledJobs() = runTest(testScheduler) {
            // Arrange
            RuntimeScheduler.start()
            val jobAfterException = AtomicBoolean(false)
            
            val now = Time.markNow()
            
            // Schedule a job that throws
            RuntimeScheduler.scheduleRecheck(testMonitor1, now.plus(3.seconds)) {
                throw RuntimeException("Test exception")
            }
            
            // Schedule another job that should still run
            RuntimeScheduler.scheduleRecheck(testMonitor2, now.plus(5.seconds)) {
                jobAfterException.set(true)
            }
            
            // Advance time past both jobs
            advanceTimeBy(6.seconds)
            advanceUntilIdle()
            
            // Assert - the second job should still execute despite the first one throwing
            assertTrue(jobAfterException.get(), "Jobs should continue to execute after an exception")
        }
    }
}