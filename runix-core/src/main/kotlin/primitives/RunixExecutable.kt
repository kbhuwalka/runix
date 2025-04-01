package runix.primitives

import runix.core.logging.primitives.RunixExecutionContext
import runix.primitives.tracing.ExecutionTrace

interface RunixExecutable {
    val name: String

    // This is the standardized entry point for the engine
    suspend fun runWithContext(context: RunixExecutionContext)

    suspend fun execute(trace: ExecutionTrace)
}