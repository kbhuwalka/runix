package runix.tracing

/**
 * Represents the lifecycle status of a traced execution unit.
 */
enum class ExecutionStatus {
    /**
     * Indicates that execution has begun.
     */
    Started,

    /**
     * Indicates successful completion of a task or action.
     */
    Success,

    /**
     * Indicates the execution was intentionally skipped, often due to preconditions not being met.
     */
    Skipped,

    /**
     * Indicates that execution failed due to an exception or error.
     */
    Failure,

    /**
     * Indicates that execution was terminated due to a timeout.
     */
    Timeout,

    /**
     * Indicates that a signal or reaction was triggered.
     */
    Triggered,

    /**
     * Indicates that execution was cancelled before completion.
     */
    Cancelled,

    /**
     * Indicates that an event completed without implying success or failure,
     * such as signal emission or passive completion of a non-critical operation.
     */
    Completed,

    Evaluated
}
