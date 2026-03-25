package runix.tracing.events

import java.time.Instant
import java.util.UUID
import kotlin.time.Duration

/**
 * Base class for all action-related trace events.
 * Provides common fields and structure for action tracing.
 */
internal sealed class ActionTraceEvent : TraceEvent {
    abstract val actionName: String
}

/**
 * Emitted when an Action is first requested but not yet started execution.
 * This marks the beginning of the action lifecycle.
 */
internal data class ActionRequested(
    override val timestamp: Instant = Instant.now(),
    override val traceId: UUID = UUID.randomUUID(),
    override val parent: TraceEvent?,
    override val actionName: String
) : ActionTraceEvent() {
    override fun toString(): String {
        return "${this.javaClass.simpleName}($actionName): [${traceId.toString().take(8)}]"
    }
}


/**
 * Emitted when an Action begins execution.
 */
internal data class ActionStarted(
    override val timestamp: Instant = Instant.now(),
    override val traceId: UUID = UUID.randomUUID(),
    override val parent: TraceEvent?,
    override val actionName: String
) : ActionTraceEvent() {
    override fun toString(): String {
        return "${this.javaClass.simpleName}($actionName, startedAt: $timestamp): [${traceId.toString().take(8)}]"
    }
}

/**
 * Emitted when an Action completes successfully.
 */
internal data class ActionSucceeded(
    override val timestamp: Instant = Instant.now(),
    override val traceId: UUID = UUID.randomUUID(),
    override val parent: TraceEvent?,
    override val actionName: String,
    val duration:  Long
) : ActionTraceEvent() {
    override fun toString(): String {
        return "${this.javaClass.simpleName}($actionName, endedAt: $timestamp, duration: $duration): [${traceId.toString().take(8)}]"
    }
}

/**
 * Emitted when an Action fails due to an exception.
 */
internal data class ActionFailed(
    override val timestamp: Instant = Instant.now(),
    override val traceId: UUID = UUID.randomUUID(),
    override val parent: TraceEvent?,
    override val actionName: String,
    val exceptionClass: String,
    val message: String?,
    val duration:  Long
) : ActionTraceEvent() {
    override fun toString(): String {
        return "${this.javaClass.simpleName}($actionName, message: $message): [${traceId.toString().take(8)}]"
    }
}

/**
 * Emitted when an Action is cancelled before completion.
 */
internal data class ActionCancelled(
    override val timestamp: Instant = Instant.now(),
    override val traceId: UUID = UUID.randomUUID(),
    override val parent: TraceEvent?,
    override val actionName: String,
    val duration:  Long
) : ActionTraceEvent() {
    override fun toString(): String {
        return "${this.javaClass.simpleName}($actionName): [${traceId.toString().take(8)}]"
    }
}

/**
 * Emitted when an Action exceeds its time limit and is forcefully terminated.
 */
internal data class ActionTimedOut(
    override val timestamp: Instant = Instant.now(),
    override val traceId: UUID = UUID.randomUUID(),
    override val parent: TraceEvent?,
    override val actionName: String,
    val timeoutDurationMillis: Long,
    val duration:  Long
) : ActionTraceEvent() {
    override fun toString(): String {
        return "${this.javaClass.simpleName}($actionName, timeout: $timeoutDurationMillis): [${traceId.toString().take(8)}]"
    }
}

/**
 * Emitted when an Action cannot be executed because it's already running.
 */
internal data class ActionRejectedAlreadyRunning(
    override val timestamp: Instant = Instant.now(),
    override val traceId: UUID = UUID.randomUUID(),
    override val parent: TraceEvent?,
    override val actionName: String
) : ActionTraceEvent() {
    override fun toString(): String {
        return "${this.javaClass.simpleName}($actionName): [${traceId.toString().take(8)}]"
    }
}