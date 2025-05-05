package runix.runtime

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import runix.primitives.monitor.MonitorHandle
import runix.runtime.internal.RuntimeScope
import runix.temporal.time.delayUntil
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.ComparableTimeMark

/**
 * Central time-aware runtime scheduler for Runix.
 *
 * - Handles recheck scheduling for long-running conditions.
 * - Avoids polling by using suspending delays.
 * - All reschedules are cancelable.
 */
internal object RuntimeScheduler {
    private var isRunning = false

    /** Scheduled rechecks indexed by monitor */
    private val recheckJobs = ConcurrentHashMap<MonitorHandle, Job>()

    private val lock = Mutex()

    fun start() {
        check(!isRunning) { "RuntimeScheduler is already running." }
        isRunning = true
    }

    fun stop() {
        if (!isRunning) return
        isRunning = false

        // Cancel all jobs
        recheckJobs.values.forEach { it.cancel() }
        recheckJobs.clear()
    }


    /**
     * Schedule a re-evaluation at the given [mark] for monitor.
     *
     * If a recheck is already pending, it is cancelled and replaced.
     */
    fun scheduleRecheck(monitor: MonitorHandle, mark: ComparableTimeMark, job: suspend () -> Unit) {
        if (!isRunning) return

        recheckJobs[monitor]?.cancel()

        val jobHandle = RuntimeScope.scope.launch {
            delayUntil(mark)
            try {
                job()
            } catch(e: Exception) {

            } finally {
                // Cleanup: remove this job if it's still the current one
                recheckJobs.remove(monitor, coroutineContext[Job])
            }
        }

        // Store the new job
        recheckJobs[monitor] = jobHandle
    }


    /**
     * Cancel any scheduled re-evaluation for the monitor.
     */
    fun cancelRecheck(monitor: MonitorHandle) {
        if (!isRunning) return

        // Remove and cancel
        recheckJobs.remove(monitor)?.cancel()
    }
}