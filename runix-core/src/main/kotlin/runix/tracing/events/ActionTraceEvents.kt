package runix.tracing.events

import java.time.Instant
import java.util.UUID
import kotlin.time.Duration

/**
 * Base class for all action-related trace events.
 * Provides common fields and structure for action tracing.
 */
sealed class ActionTraceEvent : TraceEvent {
    abstract val actionName: String
}

/**
 * Emitted when an Action is first requested but not yet started execution.
 * This marks the beginning of the action lifecycle.
 */
data class ActionRequested(
    override val timestamp: Instant = Instant.now(),
    override val traceId: UUID,
    override val parentId: UUID?,
    override val actionName: String
) : ActionTraceEvent()


/**
 * Emitted when an Action begins execution.
 */
data class ActionStarted(
    override val timestamp: Instant = Instant.now(),
    override val traceId: UUID,
    override val parentId: UUID?,
    override val actionName: String
) : ActionTraceEvent()

/**
 * Emitted when an Action completes successfully.
 */
data class ActionSucceeded(
    override val timestamp: Instant = Instant.now(),
    override val traceId: UUID,
    override val parentId: UUID?,
    override val actionName: String,
    val duration:  Long
) : ActionTraceEvent()

/**
 * Emitted when an Action fails due to an exception.
 */
data class ActionFailed(
    override val timestamp: Instant = Instant.now(),
    override val traceId: UUID,
    override val parentId: UUID?,
    override val actionName: String,
    val exceptionClass: String,
    val message: String?,
    val duration:  Long
) : ActionTraceEvent()

/**
 * Emitted when an Action is cancelled before completion.
 */
data class ActionCancelled(
    override val timestamp: Instant = Instant.now(),
    override val traceId: UUID,
    override val parentId: UUID?,
    override val actionName: String,
    val duration:  Long
) : ActionTraceEvent()

/**
 * Emitted when an Action exceeds its time limit and is forcefully terminated.
 */
data class ActionTimedOut(
    override val timestamp: Instant = Instant.now(),
    override val traceId: UUID,
    override val parentId: UUID?,
    override val actionName: String,
    val timeoutDurationMillis: Long,
    val duration:  Long
) : ActionTraceEvent()

/**
 * Emitted when an Action cannot be executed because it's already running.
 */
data class ActionRejectedAlreadyRunning(
    override val timestamp: Instant = Instant.now(),
    override val traceId: UUID,
    override val parentId: UUID?,
    override val actionName: String
) : ActionTraceEvent()