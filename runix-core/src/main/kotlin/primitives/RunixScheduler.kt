package runix.primitives

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import runix.core.lifecycle.Disposable
import runix.core.logging.logger
import runix.primitives.internal.SignalRegistry
import runix.primitives.tracing.ExecutionTrace
import runix.tracing.LiveTraceManager
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

    fun fireSignal(name: String) {
        signalRegistry.fire(name)
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

    fun <T> schedule(action: Action<T>, input: T, parentTrace: ExecutionTrace? = null) {
        val trace = childTraceFor(action.name, parentTrace)
        schedule(RunixJob(action, trace, input))
    }

    fun <T> runNow(action: Action<T>, input: T, parentTrace: ExecutionTrace? = null) {
        val trace = childTraceFor(action.name, parentTrace)
        coroutineScope.launch {
            runNow(RunixJob(action, trace, input))
        }
    }

    suspend fun <T> runNowAndWait(action: Action<T>, input: T, parentTrace: ExecutionTrace? = null): ActionResult {
        val awaiter = CompletableDeferred<ActionResult>()

        val trace = ExecutionTrace(
            parentId = parentTrace?.id,
            path = parentTrace?.path.orEmpty() + action.name,
            scheduler = this
        )

        val job = RunixJob(action, trace, input, awaiter)
        runNow(job)
        return awaiter.await()
    }

    suspend fun <T> scheduleAndWait(action: Action<T>, input: T, parentTrace: ExecutionTrace? = null): ActionResult {
        val awaiter = CompletableDeferred<ActionResult>()

        val trace = ExecutionTrace(
            parentId = parentTrace?.id,
            path = parentTrace?.path.orEmpty() + action.name,
            scheduler = this
        )

        val job = RunixJob(action, trace, input, awaiter)
        schedule(job)
        return awaiter.await()
    }

    fun schedule(reaction: Reaction, parentTrace: ExecutionTrace? = null) {
        val trace = childTraceFor(reaction.name, parentTrace)
        schedule(RunixJob(reaction, trace))
    }

    fun runNow(reaction: Reaction, parentTrace: ExecutionTrace? = null) {
        val trace = childTraceFor(reaction.name, parentTrace)
        coroutineScope.launch {
            runNow(RunixJob(reaction, trace))
        }
    }

    private fun childTraceFor(name: String, parent: ExecutionTrace?): ExecutionTrace {
        return ExecutionTrace(
            parentId = parent?.id,
            path = parent?.path.orEmpty() + name,
            scheduler = this
        )
    }
}