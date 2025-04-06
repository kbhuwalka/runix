package runix.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import runix.internal.RunixRuntimeScope
import runix.internal.SignalRegistry
import runix.primitives.Action
import runix.primitives.ActionResult
import runix.primitives.ConflictPolicy
import runix.primitives.Monitor
import runix.primitives.Reaction
import runix.primitives.RunixExecutable
import runix.primitives.RunixExecutionContext
import runix.primitives.RunixJob
import runix.primitives.Signal
import runix.temporal.ConditionEval
import runix.tracing.ExecutionStatus
import runix.tracing.ExecutionTrace
import runix.tracing.LiveTraceManager
import runix.tracing.Trace
import runix.tracing.TraceLogger
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.TimeSource

typealias CancellationCallback = (ExecutionTrace) -> Unit

class RunixScheduler(
    private val coroutineScope: CoroutineScope = RunixRuntimeScope.scope,
    val traceLogger: TraceLogger? = null,
    val traceManager: LiveTraceManager = LiveTraceManager()
) {

    private val jobQueue = Channel<RunixJob>(Channel.UNLIMITED)

    // Track currently running + queued jobs by their trace ID or string
    private val runningJobs = ConcurrentHashMap.newKeySet<String>()
    private val queuedJobs = ConcurrentHashMap.newKeySet<String>()

    private val pendingRechecks = mutableMapOf<String, Job>()

    internal val signalRegistry = SignalRegistry(this, traceLogger = traceLogger)

    private val runningActions = ConcurrentHashMap<String, RunixJob>()

    fun start() {
        coroutineScope.launch {
            for (job in jobQueue) {
                launch {
                    val key = "${job.executable.name}-${job.trace.id}"

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

    fun schedule(job: RunixJob) {
        val key = "${job.executable.name}-${job.trace.id}"
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
        val stateJob = startStateListener(monitor)

        return object : Disposable {
            override fun dispose() {
                stateJob.cancel()
            }
        }
    }

    private fun startStateListener(monitor: Monitor): Job {
        val combinedFlow = monitor.dependsOn.merge()

        return coroutineScope.launch {
            combinedFlow.collectLatest {
                handleMonitorEvaluation(monitor)
            }
        }
    }

    private fun handleMonitorEvaluation(monitor: Monitor) {
        val trace = Trace.root("Monitor(${monitor.name})", this@RunixScheduler)
        val ctx = RunixExecutionContext(trace, this@RunixScheduler, null)

        when (val result = monitor.condition()) {
            is ConditionEval.True -> {
                pendingRechecks.remove(monitor.name)?.cancel()
                monitor.evaluateWithContext(ctx)
            }

            is ConditionEval.Delayed -> {
                pendingRechecks.remove(monitor.name)?.cancel()
                pendingRechecks[monitor.name] = scheduleMonitorRecheck(monitor, result.nextCheckAt)
            }

            is ConditionEval.False -> {
                pendingRechecks.remove(monitor.name)?.cancel()
            }
        }
    }

    /**
     * Schedules a re-evaluation of the given [monitor] at a specific time.
     * Used to handle delayed condition evaluations (e.g., `.persistedFor(...)`).
     */
    private fun scheduleMonitorRecheck(monitor: Monitor, at: TimeSource.Monotonic.ValueTimeMark): Job {
        val delayDuration = at.elapsedNow()
        if (!delayDuration.isNegative() && delayDuration < Duration.ZERO) return Job() // no-op

        return coroutineScope.launch {
            delay(delayDuration)
            handleMonitorEvaluation(monitor)
        }
    }

    private fun scheduleAction(action: Action, job: RunixJob, key: String) {
        val actionName = action.name
        val existingJob = runningActions[actionName]
        val trace = job.trace

        synchronized(this) {
            when (action.conflictPolicy) {
                ConflictPolicy.SkipIfRunning -> {
                    if (existingJob?.job?.isActive == true) return
                }
                ConflictPolicy.CancelPrevious -> {
                    existingJob?.let {
                        logCancellationDueToConflict(it, job)
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
                    val cancelCallbacks = mutableListOf<CancellationCallback>()

                    try {
                        action.cancelOn.forEach { signal ->
                            val cancelCallback: CancellationCallback = { signalTrace ->
                                Trace.log(
                                    trace = trace,
                                    type = "Cancelled",
                                    status = ExecutionStatus.Cancelled,
                                    logger = traceLogger,
                                    message = "Cancelled by signal: ${signal.name}",
                                    cause = signalTrace,
                                    startTime = trace.startTime,
                                    endTime = Instant.now(),
                                    context = mapOf(
                                        "reason" to "SignalFired",
                                        "signal" to signal.name
                                    )
                                )
                                this.cancel(CancellationException("Cancelled by signal: ${signal.name}"))
                            }
                            cancelCallbacks += cancelCallback
                            signalRegistry.registerCancellationListener(signal, cancelCallback)
                        }

                        job.run(this@RunixScheduler)
                    } finally {
                        runningActions.remove(actionName)
                        action.cancelOn.forEachIndexed { index, signal ->
                            signalRegistry.unregisterCancellationListener(signal, cancelCallbacks[index])
                        }
                    }
                }

                job.job = newJob
                runningActions[actionName] = job
                newJob.start()
            }
        }
    }

    private fun logCancellationDueToConflict(target: RunixJob, causedBy: RunixJob) {
        Trace.log(
            trace = target.trace,
            type = "Cancelled",
            status = ExecutionStatus.Cancelled,
            logger = traceLogger,
            message = "Cancelled due to conflict with: ${causedBy.trace.path.lastOrNull() ?: "Unknown"}",
            cause = causedBy.trace
        )
    }

    suspend fun runNow(job: RunixJob) {
        job.run(this)
    }

    fun fireSignal(signal: Signal, parentTrace: ExecutionTrace? = null) {
        val trace = parentTrace?.let {
            Trace.child("Signal(${signal.name})", it, actor = signal.actor, tags = signal.tags)
        } ?: Trace.root("Signal(${signal.name}) [External]", this, actor = signal.actor, tags = signal.tags + "external")

        signalRegistry.fire(signal, trace)
        if (parentTrace == null) {
            Trace.log(trace, "Fired", ExecutionStatus.Completed, traceLogger, context = mapOf("origin" to "external"))
        }
    }

    // === Convenience overloads ===

    fun schedule(executable: RunixExecutable, parentTrace: ExecutionTrace? = null) {
        val trace = childTraceFor(executable.name, parentTrace)
        schedule(RunixJob(executable, trace))
    }

    fun runNow(executable: RunixExecutable, parentTrace: ExecutionTrace? = null) {
        val trace = childTraceFor(executable.name, parentTrace)
        coroutineScope.launch {
            runNow(RunixJob(executable, trace))
        }
    }

    suspend fun runNowAndWait(action: Action, parentTrace: ExecutionTrace? = null): ActionResult {
        val waiter = CompletableDeferred<ActionResult>()
        val trace = childTraceFor(action.name, parentTrace)
        val job = RunixJob(action, trace, waiter)
        runNow(job)
        return waiter.await()
    }

    private fun childTraceFor(name: String, parent: ExecutionTrace?): ExecutionTrace {
        return parent?.let { Trace.child(name, it) } ?: Trace.root(name, this)
    }
}
