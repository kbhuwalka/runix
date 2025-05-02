package runix.core

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import runix.internal.MonitorThrottleRegistry
import runix.internal.RuntimeScope
import runix.internal.SignalRegistry
import runix.internal.ThrottleResult
import runix.primitives.*
import runix.temporal.condition.ConditionEval
import runix.temporal.time.delayUntil
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.ComparableTimeMark


class RunixScheduler(
    private val coroutineScope: CoroutineScope = RuntimeScope.scope,
) {

    val scope: CoroutineScope
        get() = coroutineScope

    private val jobQueue = Channel<RunixJob>(Channel.UNLIMITED)

    // Track currently running + queued jobs by their trace ID or string
    private val runningJobs = ConcurrentHashMap.newKeySet<String>()
    private val queuedJobs = ConcurrentHashMap.newKeySet<String>()

    private val pendingRechecks = mutableMapOf<String, Job>()

    internal val signalRegistry = SignalRegistry(this)

    private val runningActions = ConcurrentHashMap<String, RunixJob>()

    fun start() {
        coroutineScope.launch {
            for (job in jobQueue) {
                launch {
                    val key = "${job.executable.name}-${UUID.randomUUID()}"

                    synchronized(this@RunixScheduler) {
                        runningJobs.add(key)
                        queuedJobs.remove(key)
                    }

                    try {
                        job.run(this@RunixScheduler)
                    } catch (e: Exception) {
                        logger.error(e) { "❌ Error executing job: $key" }
                    } finally {
                        synchronized(this@RunixScheduler) {
                            runningJobs.remove(key)
                            if (queuedJobs.contains(key)) {
                                jobQueue.trySend(job)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun schedule(job: RunixJob) {
        val key = "${job.executable.name}-${UUID.randomUUID()}"
        val action = job.executable as? Action

        if (action != null) {
            scheduleAction(action, job, key)
            return
        }

        synchronized(this) {
            // If the job is already running, requeue it and return
            if (runningJobs.contains(key)) {
                queuedJobs.add(key)
                return
            }

            // If the job is not already queued, add and launch it
            if (queuedJobs.add(key)) {
                coroutineScope.launch {
                    job.run(this@RunixScheduler)
                }
            }
        }
    }

    /**
     * Registers a [Reaction] to listen for its declared signals.
     * Returns a [Disposable] to clean up the subscriptions.
     */
    fun register(reaction: Reaction): Disposable {
        val registeredSignals = reaction.signalNames

        registeredSignals.forEach { signalRegistry.subscribe(it, reaction) }

        return object : Disposable {
            override fun dispose() {
                registeredSignals.forEach { signalRegistry.unsubscribe(it, reaction) }
            }
        }
    }

    fun register(monitor: Monitor): Disposable {
        val compiled = monitor.conditionTree.compile(monitor.name)

        compiled.start {
            requestImmediateEvaluation(monitor)
        }
        monitor.condition = compiled.condition

        return object : Disposable {
            override fun dispose() {
                compiled.stop()
            }
        }
    }

    private fun requestImmediateEvaluation(monitor: Monitor) {
        scope.launch {
            handleMonitorEvaluation(monitor)
        }
    }

    private fun handleMonitorEvaluation(monitor: Monitor) {
        if (!monitor.isEnabled()) return

        val ctx = RunixExecutionContext( this, null)
        val context = MonitorContext.from(ctx, monitor.name)

        val result = try {
            monitor.condition()
        } catch (e: Exception) {
            return
        }


        when (result) {
            is ConditionEval.True -> {
                val cooldown = monitor.throttleInterval
                if (cooldown != null) {
                    when (val throttle = MonitorThrottleRegistry.peek(monitor.name, cooldown)) {
                        is ThrottleResult.Throttled -> {
                            return
                        }
                        ThrottleResult.Allow -> {
                            // Proceed to trigger
                        }
                    }
                }

                monitor.onTriggered(context)
                // ✅ Record trigger *after* successful fire
                if (cooldown != null) {
                    MonitorThrottleRegistry.recordTrigger(monitor.name)
                }
            }

            is ConditionEval.Delayed -> {
                pendingRechecks.remove(monitor.name)?.cancel()
                pendingRechecks[monitor.name] = scheduleMonitorRecheck(monitor, result.nextCheckAt)
                monitor.onSkipped(context)
            }

            is ConditionEval.False -> {
                monitor.onSkipped(context)
            }
        }
    }

    /**
     * Schedules a re-evaluation of the given [monitor] at a specific time.
     * Used to handle delayed condition evaluations (e.g., `.persistedFor(...)`).
     */
    private fun scheduleMonitorRecheck(
        monitor: Monitor,
        at: ComparableTimeMark): Job {
        return coroutineScope.launch {
            delayUntil(at)
            handleMonitorEvaluation(monitor)
        }
    }

    private fun scheduleAction(action: Action, job: RunixJob, key: String) {
        val actionName = action.name
        val existingJob = runningActions[actionName]

        synchronized(this) {
            when (action.conflictPolicy) {
                ConflictPolicy.SkipIfRunning -> {
                    if (existingJob?.job?.isActive == true) return
                }
                ConflictPolicy.CancelPrevious -> {
                    existingJob?.let {
                        it.job?.cancel(CancellationException("Cancelled due to new conflicting action"))
                    }
                }
                else -> { /* Allow */ }
            }

            if (runningJobs.contains(key)) {
                queuedJobs.add(key)
                return
            }

            if (queuedJobs.add(key)) {
                val newJob = coroutineScope.launch(start = CoroutineStart.LAZY) {
                    try {
                        job.run(this@RunixScheduler)
                    } finally {
                        runningActions.remove(actionName)
                    }
                }

                job.job = newJob
                runningActions[actionName] = job
                newJob.start()
            }
        }
    }

    fun fireSignal(signal: Signal) {
        signalRegistry.fire(signal)
    }

    // === Convenience overloads ===

    fun run(executable: RunixExecutable) {
        schedule(RunixJob(executable))
    }

    suspend fun runAndWait(action: Action): ActionResult {
        val waiter = CompletableDeferred<ActionResult>()
        val job = RunixJob(action, waiter)
        job.run(this)
        return waiter.await()
    }
}
