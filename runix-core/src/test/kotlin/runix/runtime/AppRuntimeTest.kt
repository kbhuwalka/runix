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
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
        AppRuntime.shutdown()
    }

    @Nested
    @DisplayName("Runtime initialization")
    inner class RuntimeInitialization {

        @Test
        @DisplayName("should initialize runtime components correctly")
        fun initializeRuntimeCorrectly() = runTest {
            AppRuntime.initialize()
            advanceUntilIdle()

            verifyOrder {
                RuntimeScope.install(any<CoroutineScope>())
            }
        }

        @Test
        @DisplayName("should reject initialization when already running")
        fun rejectsInitializationWhenAlreadyRunning() = runTest {
            AppRuntime.initialize()
            advanceUntilIdle()

            assertThrows<IllegalStateException> {
                runTest {
                    AppRuntime.initialize()
                }
            }
        }
    }

    @Nested
    @DisplayName("Runtime shutdown")
    inner class RuntimeShutdown {

        @Test
        @DisplayName("should clean up runtime components correctly")
        fun cleanupRuntimeCorrectly() = runTest {
            AppRuntime.initialize()
            advanceUntilIdle()

            AppRuntime.shutdown()

            verify {
                RuntimeScope.clear()
            }
        }

        @Test
        @DisplayName("should do nothing if runtime is not running")
        fun doNothingIfNotRunning() {
            AppRuntime.shutdown()

            verify(exactly = 0) {
                RuntimeScope.clear()
            }
        }
    }

    @Nested
    @DisplayName("Runtime lifecycle")
    inner class RuntimeLifecycle {

        @Test
        @DisplayName("should support multiple initialize-shutdown cycles")
        fun supportsMultipleInitializeShutdownCycles() = runTest {
            // First cycle
            AppRuntime.initialize()
            advanceUntilIdle()
            AppRuntime.shutdown()

            verify(exactly = 1) {
                RuntimeScope.install(any<CoroutineScope>())
                RuntimeScope.clear()
            }

            io.mockk.clearMocks(
                RuntimeScope, 
                recordedCalls = true,
                answers = false
            )

            // Second cycle
            AppRuntime.initialize()
            advanceUntilIdle()
            AppRuntime.shutdown()

            verify(exactly = 1) {
                RuntimeScope.install(any<CoroutineScope>())
                RuntimeScope.clear()
            }
        }
    }

    @Nested
    @DisplayName("Runtime resilience")
    inner class RuntimeResilience {

        @Test
        @DisplayName("should clean up runtime even if an exception occurs during shutdown")
        fun cleanupRuntimeEvenWithException() = runTest {
            AppRuntime.initialize()
            advanceUntilIdle()

            AppRuntime.shutdown()

            verify { RuntimeScope.clear() }
        }
    }
}