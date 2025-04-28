package runix.temporal.trackers

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import runix.temporal.time.TestSchedulerTimeProvider
import runix.temporal.time.Time
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class ValueHistoryTest {
    private val scheduler = TestCoroutineScheduler()
    private val provider = TestSchedulerTimeProvider(scheduler)

    @BeforeTest
    fun setup() {
        Time.setProvider(provider)
    }

    @AfterTest
    fun tearDown() {
        Time.resetToRealTime()
    }

    @Test
    fun `seed only contains initial event`() = runTest(scheduler) {
        val t0 = Time.markNow()
        val history = ValueHistory(ValueWithMark("A", t0), retention = 5.seconds)

        val entries = history.entries().toList()
        assertEquals(listOf("A"), entries.map { it.value })
        assertEquals(listOf(t0), entries.map { it.timestamp })
    }

    @Test
    fun `zero retention clears history on append`() = runTest(scheduler) {
        val t0 = Time.markNow()
        val history = ValueHistory(ValueWithMark("A", t0), retention = 0.seconds)

        advanceTimeBy(1.seconds)
        // flush any pending tasks (none for ValueHistory)
        val t1 = Time.markNow()
        history.append("B", t1)

        val entries = history.entries().toList()
        assertEquals(listOf("B"), entries.map { it.value })
        assertEquals(listOf(t1), entries.map { it.timestamp })
    }

    @Test
    fun `no-change append does not add`() = runTest(scheduler) {
        val t0 = Time.markNow()
        val history = ValueHistory(ValueWithMark("A", t0), retention = 5.seconds)

        advanceTimeBy(1.seconds)
        val t1 = Time.markNow()
        history.append("A", t1)

        val entries = history.entries().toList()
        assertEquals(listOf("A"), entries.map { it.value })
        assertEquals(listOf(t0), entries.map { it.timestamp })
    }

    @Test
    fun `simple append adds new entry`() = runTest(scheduler) {
        val t0 = Time.markNow()
        val history = ValueHistory(ValueWithMark("A", t0), retention = 5.seconds)

        advanceTimeBy(1.seconds)
        val t1 = Time.markNow()
        history.append("B", t1)

        val entries = history.entries().toList()
        assertEquals(listOf("A", "B"), entries.map { it.value })
        assertEquals(listOf(t0, t1), entries.map { it.timestamp })
    }

    @Test
    fun `prune nothing when retention not expired`() = runTest(scheduler) {
        val t0 = Time.markNow()
        val history = ValueHistory(ValueWithMark("A", t0), retention = 10.seconds)

        advanceTimeBy(2.seconds)
        val t1 = Time.markNow()
        history.append("B", t1)

        advanceTimeBy(5.seconds)
        val t2 = Time.markNow()
        history.prune(t2)

        val entries = history.entries().toList()
        assertEquals(listOf("A", "B"), entries.map { it.value })
    }

    @Test
    fun `prune first only when expired`() = runTest(scheduler) {
        val t0 = Time.markNow()
        val history = ValueHistory(ValueWithMark("A", t0), retention = 2.seconds)

        advanceTimeBy(1.seconds)
        val t1 = Time.markNow()
        history.append("B", t1)

        advanceTimeBy(2.seconds)
        val t2 = Time.markNow()
        history.prune(t2)

        val entries = history.entries().toList()
        assertEquals(listOf("B"), entries.map { it.value })
    }

    @Test
    fun `prune boundary keeps first for overlap`() = runTest(scheduler) {
        val t0 = Time.markNow()
        val history = ValueHistory(ValueWithMark("A", t0), retention = 1.seconds)

        advanceTimeBy(1.seconds)
        val t1 = Time.markNow()
        history.append("B", t1)

        // prune at t1 = boundary
        history.prune(t1)

        val entries = history.entries().toList()
        assertEquals(listOf("A", "B"), entries.map { it.value })
    }

    @Test
    fun `multiple expirations drop all previous`() = runTest(scheduler) {
        val t0 = Time.markNow()
        val history = ValueHistory(ValueWithMark("A", t0), retention = 2.seconds)

        advanceTimeBy(1.seconds)
        val t1 = Time.markNow()
        history.append("B", t1)

        advanceTimeBy(1.seconds)
        val t2 = Time.markNow()
        history.append("C", t2)

        advanceTimeBy(3.seconds)
        val t3 = Time.markNow()
        history.prune(t3)

        val entries = history.entries().toList()
        assertEquals(listOf("C"), entries.map { it.value })
    }

    @Test
    fun `mixed append and prune sequence`() = runTest(scheduler) {
        val t0 = Time.markNow()
        val history = ValueHistory(ValueWithMark("A", t0), retention = 3.seconds)

        advanceTimeBy(1.seconds)
        val t1 = Time.markNow()
        history.append("B", t1)

        advanceTimeBy(1.seconds)
        val t2 = Time.markNow()
        history.append("C", t2)

        advanceTimeBy(2.seconds)
        val t3 = Time.markNow()
        history.prune(t3)

        val entries = history.entries().toList()
        assertEquals(listOf("B", "C"), entries.map { it.value })
    }

    @Test
    fun `negative retention constructor throws`() {
        assertFailsWith<IllegalArgumentException> {
            ValueHistory(ValueWithMark("X", Time.markNow()), retention = (-1).seconds)
        }
    }
}
