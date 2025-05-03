package runix.runtime

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import runix.internal.RuntimeScope
import runix.primitives.monitor.MonitorHandle
import runix.temporal.time.delayUntil
import kotlin.time.ComparableTimeMark

/**
 * Central time-aware runtime scheduler for Runix.
 *
 * - Handles recheck scheduling for long-running conditions.
 * - Avoids polling by using suspending delays.
 * - All reschedules are cancelable.
 */
internal object RuntimeScheduler {
    private val scope = RuntimeScope.scope

    /** Scheduled rechecks indexed by monitor name */
    private val recheckJobs = mutableMapOf<MonitorHandle, Job>()
    private val lock = Mutex()

    /**
     * Schedule a re-evaluation at the given [mark] for [monitorId].
     *
     * If a recheck is already pending, it is cancelled and replaced.
     */
    fun scheduleRecheck(monitor: MonitorHandle, mark: ComparableTimeMark, job: suspend () -> Unit) {
        scope.launch {
            lock.withLock {
                recheckJobs[monitor]?.cancel()
                val jobHandle = launch {
                    delayUntil(mark)
                    job()
                }
                recheckJobs[monitor] = jobHandle
            }
        }
    }

    /**
     * Cancel any scheduled re-evaluation for [monitorId].
     */
    fun cancelRecheck(monitor: MonitorHandle) {
        scope.launch {
            lock.withLock {
                recheckJobs.remove(monitor)?.cancel()
            }
        }
    }

    /**
     * Gracefully stop the scheduler and clear pending tasks.
     */
    fun shutdown() {
        scope.cancel("RuntimeScheduler shutdown")
        recheckJobs.clear()
    }
}