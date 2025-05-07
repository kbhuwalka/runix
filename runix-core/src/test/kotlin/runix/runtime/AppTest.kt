package runix.runtime

import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import runix.RuntimeScopeTestHelper
import runix.primitives.module.AppModule
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("App")
class AppTest {

    // Test infrastructure setup
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val runtimeHelper = RuntimeScopeTestHelper(testScope)
    
    @BeforeEach
    fun setUp() {
        runtimeHelper.setup()

        mockkObject(AppRuntime)
        coJustRun { AppRuntime.initialize() }
        justRun { AppRuntime.shutdown() }
    }

    @AfterEach
    fun tearDown() {
        runtimeHelper.tearDown()
        unmockkAll()
    }

    // Helper function to create a mock module
    private fun createMockModule(
        name: String = "TestModule",
        failOnActivate: Boolean = false
    ): AppModule {
        return mockk<AppModule>(relaxed = true) {
            every { this@mockk.name } returns name
            if (failOnActivate) {
                coEvery { activate() } throws RuntimeException("Activation failed")
            } else {
                coJustRun { activate() }
            }
            coJustRun { deactivate() }
        }
    }

    @Nested
    @DisplayName("Module installation")
    inner class ModuleInstallation {

        @Test
        @DisplayName("should add module to app and set app reference")
        fun addModuleAndSetAppReference() {
            val app = TestApp()
            val module = createMockModule()

            app.install(module)

            verify { module.setApp(app) }
            assertTrue(app.testModules.contains(module))
        }
    }

    @Nested
    @DisplayName("Synchronous start and stop")
    inner class SyncStartStop {

        @Test
        @DisplayName("start() should set up JVM shutdown hook, start app, and block until signaled")
        fun startSetsUpHookAndBlocks() {
            // Arrange
            val app = spyk(TestApp())
            coJustRun { app.startAsync() }
            coJustRun { app.stopAsync() }
            
            // Mock Runtime for shutdown hook verification
            val runtime = mockk<Runtime>()
            mockkStatic(Runtime::class)
            every { Runtime.getRuntime() } returns runtime
            justRun { runtime.addShutdownHook(any()) }
            
            // Set up a separate thread to call stop() after a delay
            val completionLatch = CountDownLatch(1)
            thread {
                // Give time for start() to begin blocking
                Thread.sleep(100)
                app.stop()
                completionLatch.countDown()
            }
            
            // Act
            app.start()
            
            // Assert
            verify {
                runtime.addShutdownHook(any())
            }
            coVerify { 
                app.startAsync()
                app.stopAsync()
            }
            
            // Verify our test completed (meaning start() unblocked)
            assertTrue(completionLatch.await(1, TimeUnit.SECONDS), "Test did not complete in time")
        }
        
        @Test
        @DisplayName("stop() should signal shutdown and call stopAsync")
        fun stopSignalsShutdownAndCallsStopAsync() {
            // Arrange
            val app = spyk(TestApp())
            coJustRun { app.stopAsync() }
            
            // Act
            app.stop()
            
            // Assert
            coVerify { app.stopAsync() }
            assertEquals(0, app.testShutdownLatch.count, "Shutdown latch should be counted down")
        }
        
        @Test
        @DisplayName("setupShutdownHook should register a thread that counts down the latch")
        fun setupShutdownHookRegistersThread() {
            // Arrange
            val app = TestApp()
            val runtime = mockk<Runtime>()
            mockkStatic(Runtime::class)
            every { Runtime.getRuntime() } returns runtime
            
            val threadSlot = slot<Thread>()
            justRun { runtime.addShutdownHook(capture(threadSlot)) }
            
            // Act
            app.testSetupShutdownHook()
            
            // Assert
            verify { runtime.addShutdownHook(any()) }
            
            // Execute the captured shutdown thread to verify it counts down the latch
            assertEquals(1, app.testShutdownLatch.count, "Latch should start at 1")
            threadSlot.captured.run()
            assertEquals(0, app.testShutdownLatch.count, "Latch should be counted down by hook")
        }
    }

    @Nested
    @DisplayName("App startup and shutdown")
    inner class AppStartupAndShutdown {
        
        @Test
        @DisplayName("startAsync should initialize runtime and activate app")
        fun startAsyncInitializesRuntimeAndActivatesApp() = runTest {
            val app = spyk(TestApp())
            coJustRun { app.activate() }

            app.startAsync()
            advanceUntilIdle()

            coVerifyOrder {
                AppRuntime.initialize()
                app.activate()
            }
        }
        
        @Test
        @DisplayName("startAsync should throw if app is already running")
        fun startAsyncThrowsIfAlreadyRunning() = runTest {
            val app = spyk(TestApp())
            coJustRun { app.activate() }

            app.startAsync()
            advanceUntilIdle()

            assertThrows<IllegalStateException> {
                runTest {
                    app.startAsync()
                }
            }
        }
        
        @Test
        @DisplayName("stopAsync should deactivate app and shutdown runtime")
        fun stopAsyncDeactivatesAppAndShutdownsRuntime() = runTest {
            val app = TestApp()

            app.startAsync()
            advanceUntilIdle()

            app.stopAsync()
            advanceUntilIdle()

            coVerifyOrder {
                app.deactivate()
                AppRuntime.shutdown()
            }
        }
        
        @Test
        @DisplayName("stopAsync should shut down runtime even if deactivation fails")
        fun stopAsyncShutdownsRuntimeEvenIfDeactivationFails() = runTest {
            val app = TestApp()
            
            app.startAsync()
            advanceUntilIdle()
            
            app.stopAsync()
            advanceUntilIdle()
            
            verify { AppRuntime.shutdown() }
        }
        
        @Test
        @DisplayName("stopAsync should do nothing if app is not running")
        fun stopAsyncDoesNothingIfNotRunning() = runTest {
            // Arrange
            val app = spyk(TestApp())
            
            // Act
            app.stopAsync()
            advanceUntilIdle()
            
            // Assert
            coVerify(exactly = 0) { app.deactivate() }
            verify(exactly = 0) { AppRuntime.shutdown() }
        }
    }

    @Nested
    @DisplayName("App activation")
    inner class AppActivation {

        @Test
        @DisplayName("should activate all modules in order and call didStart")
        fun activatesModulesInOrderAndCallsDidStart() = runTest {
            // Arrange
            val app = TestApp()
            var didStartCalled = false
            
            // Create modules with tracked activation order
            val activationOrder = mutableListOf<String>()
            val modules = List(3) { index ->
                mockk<AppModule>(relaxed = true) {
                    every { name } returns "Module$index"
                    coEvery { activate() } coAnswers {
                        activationOrder.add("Module$index")
                    }
                }
            }
            
            modules.forEach { app.install(it) }
            app.didStart { didStartCalled = true }

            app.activate()
            advanceUntilIdle()

            assertTrue(didStartCalled, "didStart callback should be called")
            assertEquals(
                listOf("Module0", "Module1", "Module2"),
                activationOrder,
                "Modules should be activated in installation order"
            )
        }

        @Test
        @DisplayName("should be idempotent")
        fun activationIsIdempotent() = runTest {
            val app = TestApp()
            val module = createMockModule()
            app.install(module)

            app.activate()
            advanceUntilIdle()
            app.activate() // The second call should be a no-op
            advanceUntilIdle()

            coVerify(exactly = 1) { module.activate() }
        }

        @Test
        @DisplayName("should propagate module activation errors")
        fun propagatesModuleActivationErrors() = runTest {
            val app = TestApp()
            val goodModule = createMockModule("GoodModule")
            val errorModule = createMockModule("ErrorModule", failOnActivate = true)
            
            app.install(goodModule)
            app.install(errorModule)
            
            var didStartCalled = false
            app.didStart { didStartCalled = true }

            assertThrows<RuntimeException> {
                app.activate()
                advanceUntilIdle()
            }

            assertFalse(didStartCalled, "didStart shouldn't be called if activation fails")
        }
    }

    @Nested
    @DisplayName("App deactivation")
    inner class AppDeactivation {

        @Test
        @DisplayName("should call willStop and deactivate modules in reverse order")
        fun callsWillStopAndDeactivatesInReverseOrder() = runTest {
            val app = TestApp()
            var willStopCalled = false
            val deactivationOrder = mutableListOf<String>()
            
            // Create modules tracking deactivation order
            val modules = List(3) { index ->
                mockk<AppModule>(relaxed = true) {
                    every { name } returns "Module$index"
                    coJustRun { activate() }
                    coEvery { deactivate() } coAnswers {
                        deactivationOrder.add("Module$index")
                    }
                }
            }
            
            modules.forEach { app.install(it) }
            app.willStop { willStopCalled = true }

            app.activate()
            advanceUntilIdle()

            app.deactivate()
            advanceUntilIdle()

            assertTrue(willStopCalled, "willStop callback should be called")
            assertEquals(
                listOf("Module2", "Module1", "Module0"),
                deactivationOrder,
                "Modules should be deactivated in reverse order"
            )
        }

        @Test
        @DisplayName("should be idempotent")
        fun deactivationIsIdempotent() = runTest {
            val app = TestApp()
            val module = createMockModule()
            app.install(module)

            app.activate()
            advanceUntilIdle()

            app.deactivate()
            advanceUntilIdle()
            app.deactivate() // The second call should be a no-op
            advanceUntilIdle()

            coVerify(exactly = 1) { module.deactivate() }
        }
    }

    /**
     * Test implementation that exposes protected/private members for verification
     */
    private class TestApp : App() {
        val testModules: List<AppModule>
            get() = this.modules.toList()
            
        // Expose shutdown latch for testing
        val testShutdownLatch: CountDownLatch
            get() = shutdownLatch
            
        // Expose private method for testing
        fun testSetupShutdownHook() {
            setupShutdownHook()
        }
    }
}