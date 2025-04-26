package runix.temporal

import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BooleanTrackerTest {

    @Test
    fun `BooleanTracker should initialize with correct lastValue`() {
        val initialFlow = MutableStateFlow(false)
        val tracker = BooleanTracker(initialFlow) { /* no-op */ }

        assertEquals(false, tracker.lastValue, "Tracker should match initial flow value on creation")
    }
}