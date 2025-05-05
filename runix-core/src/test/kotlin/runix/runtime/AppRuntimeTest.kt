package runix.runtime

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import runix.runtime.internal.RuntimeScope

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("AppRuntime")
class AppRuntimeTest {

    // Test infrastructure setup
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @BeforeEach
    fun setup() {
        // Mock RuntimeScope
        mockkObject(RuntimeScope)
        every { RuntimeScope.scope } returns testScope
        justRun { RuntimeScope.install(any()) }
        justRun { RuntimeScope.clear() }

        // Mock RuntimeScheduler
        mockkObject(RuntimeScheduler)
        justRun { RuntimeScheduler.start() }
        justRun { RuntimeScheduler.stop() }
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
        runBlocking { AppRuntime.stop() }
    }

    // Helper function to create mock app
    private fun createMockApp(throwOnDeactivate: Boolean = false): App {
        return mockk<App>(relaxed = true) {
            coJustRun { activate() }
            if (throwOnDeactivate) {
                coEvery { deactivate() } throws RuntimeException("Deactivation failed")
            } else {
                coJustRun { deactivate() }
            }
        }
    }

    @Nested
    @DisplayName("Starting an app")
    inner class StartApp {

        @Test
        @DisplayName("should initialize runtime components and activate app")
        fun initializeRuntimeAndActivateApp() = runTest {
            val app = createMockApp()

            AppRuntime.start(app)
            advanceUntilIdle()

            verifyOrder {
                RuntimeScope.install(any<CoroutineScope>())
                RuntimeScheduler.start()
            }
            coVerify { app.activate() }
        }

        @Test
        @DisplayName("should reject starting when already running")
        fun rejectsStartingWhenAlreadyRunning() = runTest {
            val firstApp = createMockApp()
            AppRuntime.start(firstApp)
            advanceUntilIdle()

            val secondApp = createMockApp()
            assertThrows<IllegalStateException> {
                runBlocking {
                    AppRuntime.start(secondApp)
                }
            }
            advanceUntilIdle()
        }
    }

    @Nested
    @DisplayName("Stopping an app")
    inner class StopApp {

        @Test
        @DisplayName("should deactivate app and clean up runtime")
        fun deactivateAppAndCleanupRuntime() = runTest {
            val app = createMockApp()
            AppRuntime.start(app)
            advanceUntilIdle()

            AppRuntime.stop()
            advanceUntilIdle()

            coVerify { app.deactivate() }
            verify { 
                RuntimeScheduler.stop()
                RuntimeScope.clear()
            }
        }

        @Test
        @DisplayName("should do nothing if runtime is not running")
        fun doNothingIfNotRunning() = runTest {
            val app = createMockApp()

            AppRuntime.stop()
            advanceUntilIdle()

            coVerify(exactly = 0) { app.deactivate() }
            verify(exactly = 0) { 
                RuntimeScheduler.stop()
                RuntimeScope.clear()
            }
        }

        @Test
        @DisplayName("should clean up runtime even if app deactivation fails")
        fun cleanupRuntimeEvenIfDeactivationFails() = runTest {
            val app = createMockApp(throwOnDeactivate = true)
            AppRuntime.start(app)
            advanceUntilIdle()

            try {
                AppRuntime.stop()
                advanceUntilIdle()
            } catch (e: RuntimeException) {
                // Expected exception from app deactivation, continue to verification
            }



            // Assert - scheduler and runtime scope should still be cleaned up
            verify { 
                RuntimeScheduler.stop()
                RuntimeScope.clear()
            }
        }
    }

    @Nested
    @DisplayName("Stopping the running app")
    inner class StopRunningApp {

        @Test
        @DisplayName("should stop the currently running app")
        fun stopsCurrentlyRunningApp() = runTest {
            val app = createMockApp()
            AppRuntime.start(app)
            advanceUntilIdle()

            AppRuntime.stop()
            advanceUntilIdle()

            coVerify { app.deactivate() }
            verify { 
                RuntimeScheduler.stop()
                RuntimeScope.clear()
            }
        }

        @Test
        @DisplayName("should do nothing if no app is running")
        fun doNothingIfNoAppRunning() = runTest {
            // Act - with no app started
            AppRuntime.stop()
            advanceUntilIdle()

            // Assert - verify no actions were taken
            verify(exactly = 0) { 
                RuntimeScheduler.stop()
                RuntimeScope.clear()
            }
        }
    }

    @Nested
    @DisplayName("Runtime lifecycle")
    inner class RuntimeLifecycle {

        @Test
        @DisplayName("should support multiple start-stop cycles")
        fun supportsMultipleStartStopCycles() = runTest {
            // First cycle
            val app1 = createMockApp()
            AppRuntime.start(app1)
            advanceUntilIdle()
            AppRuntime.stop()
            advanceUntilIdle()

            // Second cycle
            val app2 = createMockApp()
            AppRuntime.start(app2)
            advanceUntilIdle()
            AppRuntime.stop()
            advanceUntilIdle()

            // Verify both apps were activated and deactivated
            coVerify { 
                app1.activate() 
                app1.deactivate()
                app2.activate() 
                app2.deactivate()
            }
        }
    }
}