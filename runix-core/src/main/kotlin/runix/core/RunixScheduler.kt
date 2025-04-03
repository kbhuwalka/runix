package runix.core

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import runix.internal.SignalRegistry
import runix.primitives.Action
import runix.primitives.ActionResult
import runix.primitives.Monitor
import runix.primitives.Reaction
import runix.primitives.RunixExecutable
import runix.primitives.RunixExecutionContext
import runix.primitives.RunixJob
import runix.primitives.Signal
import runix.temporal.ConditionEval
import runix.tracing.ExecutionTrace
import runix.tracing.LiveTraceManager
import runix.tracing.Trace
import runix.tracing.TraceLogger
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class RunixScheduler(
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    val traceLogger: TraceLogger? = null,
    val traceManager: LiveTraceManager = LiveTraceManager()
) {

    private val jobQueue = Channel<RunixJob>(Channel.UNLIMITED)

    // Track currently running + queued jobs by their trace ID or string
    private val runningJobs = ConcurrentHashMap.newKeySet<String>()
    private val queuedJobs = ConcurrentHashMap.newKeySet<String>()

    private val pendingRechecks = mutableMapOf<String, Job>()

    private val signalRegistry = SignalRegistry(this, traceLogger = traceLogger)

    fun start() {
        coroutineScope.launch {
            for (job in jobQueue) {
                launch {
                    val key = job.trace.toString()

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
        val key = job.trace.toString()

        synchronized(this) {
            if (runningJobs.contains(key)) {
                queuedJobs.add(key)
                return
            }

            if (queuedJobs.add(key)) {
                jobQueue.trySend(job)
            }
        }
    }

    suspend fun runNow(job: RunixJob) {
        job.run(this)
    }

    fun fireSignal(signal: Signal, parentTrace: ExecutionTrace? = null) {
        signalRegistry.fire(signal, parentTrace)
    }

    // Register a Reaction with state and signal watchers
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
        val combinedFlow = monitor.dependsOn
            .map { it.map { } }
            .merge()

        return coroutineScope.launch {
            combinedFlow.collectLatest {
                val trace = Trace.root("Monitor(${monitor.name})", this@RunixScheduler)
                val ctx = RunixExecutionContext(trace, this@RunixScheduler, null)

                when (val result = monitor.condition()) {
                    is ConditionEval.True -> {
                        pendingRechecks.remove(monitor.name)?.cancel()
                        monitor.evaluateWithContext(ctx)
                    }

                    is ConditionEval.Delayed -> {
                        pendingRechecks.remove(monitor.name)?.cancel() // cancel before scheduling next
                        pendingRechecks[monitor.name] = scheduleMonitorRecheck(monitor, result.nextCheckAt)
                    }

                    is ConditionEval.False -> {
                        pendingRechecks.remove(monitor.name)?.cancel() // 🔥 Only here should we cancel without rescheduling
                    }
                }
            }
        }
    }

    private fun scheduleMonitorRecheck(monitor: Monitor, at: Instant): Job {
        val delayMs = at.toEpochMilli() - System.currentTimeMillis()
        if (delayMs <= 0) return Job() // noop

        return coroutineScope.launch {
            delay(delayMs)
            val trace = Trace.root("MonitorRecheck(${monitor.name})", this@RunixScheduler)
            val ctx = RunixExecutionContext(trace, this@RunixScheduler, null)
            monitor.evaluateWithContext(ctx)
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
