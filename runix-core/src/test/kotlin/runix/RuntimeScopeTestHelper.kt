package runix

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import runix.runtime.internal.RuntimeScope

/**
 * Helper class for setting up and tearing down RuntimeScope in tests.
 * This simplifies the common pattern of mocking RuntimeScope.scope and async calls.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RuntimeScopeTestHelper(
    private val testScope: TestScope
) {
    /**
     * Sets up RuntimeScope mocking for testing async operations.
     * - Mocks RuntimeScope.scope to return the test scope
     * - Mocks RuntimeScope.scope.async to return immediately completed deferred values
     */
    fun setup() {
        mockkObject(RuntimeScope)
        every { RuntimeScope.scope } returns testScope
    }

    /**
     * Cleans up all mocks created by this helper.
     */
    fun tearDown() {
        unmockkAll()
    }
}