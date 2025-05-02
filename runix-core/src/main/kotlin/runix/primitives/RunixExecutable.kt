package runix.primitives

interface RunixExecutable {
    val name: String

    // This is the standardized entry point for the engine
    suspend fun runWithContext(context: RunixExecutionContext)

    suspend fun execute()
}
