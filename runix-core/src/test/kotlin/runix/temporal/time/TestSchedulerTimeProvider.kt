package runix.temporal.time

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import java.time.Instant
import kotlin.time.ComparableTimeMark
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class TestSchedulerTimeProvider(
    private val scheduler: TestCoroutineScheduler
) : TimeProvider {
    override fun markNow(): ComparableTimeMark =
        // Delegate monotonic marks to the scheduler’s built-in clock
        scheduler.timeSource.markNow()

    override fun nowWallClock(): Instant =
        // In virtual mode, wall-clock isn’t used; return epoch or real time if you prefer
        Instant.EPOCH
}
