package runix.primitives

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import runix.core.lifecycle.Disposable
import runix.core.logger
import runix.core.logging.primitives.RunixExecutionContext
import runix.primitives.internal.SignalRegistry
import runix.primitives.tracing.ExecutionTrace
import runix.tracing.LiveTraceManager
import runix.tracing.Trace
import runix.tracing.TraceLogger
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

    private val signalRegistry = SignalRegistry(this, logger = traceLogger)

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
        val stateJob = startStateListener(reaction)

        reaction.signalNames.forEach { signalRegistry.subscribe(it, reaction) }

        return object : Disposable {
            override fun dispose() {
                stateJob.cancel()
            }
        }
    }

    fun register(monitor: Monitor): Disposable {
        val stateJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                delay(1000) // or whatever polling/check interval
                val context = RunixExecutionContext(
                    trace = ExecutionTrace(
                        path = listOf("Monitor(${monitor.name})"),
                        scheduler = this@RunixScheduler
                    ),
                    scheduler = this@RunixScheduler,
                    awaiter = null
                )
                monitor.evaluateWithContext(context)
            }
        }

        return object : Disposable {
            override fun dispose() {
                stateJob.cancel()
            }
        }
    }

    private fun startStateListener(reaction: Reaction): Job {
        val combinedFlow = combine(reaction.dependsOn.map { it.flow }) { values ->
            Pair(values, reaction.condition())
        }
            .distinctUntilChanged { old, new -> old.second == new.second }
            .filter { it.second }
            .map { it.first }

        return coroutineScope.launch {
            combinedFlow.collectLatest { values ->
                val trace = ExecutionTrace(
                    parentId = null,
                    path = listOf(reaction.name),
                    scheduler = this@RunixScheduler
                )
                logReactionTrigger(reaction, values)
                schedule(RunixJob(reaction, trace))
            }
        }
    }

    private fun logReactionTrigger(reaction: Reaction, values: Array<Any?>) {
        val log = buildString {
            appendLine("🔁 Reaction [${reaction.name}] triggered.")
            reaction.dependsOn.forEachIndexed { i, flow ->
                appendLine("  ↳ ${flow.name} = ${values.getOrNull(i)}")
            }
        }
        logger.info { log }
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