package runix.primitives.action

import kotlin.time.Duration
import kotlin.time.Duration.Companion.INFINITE

/**
 * Configuration options for creating actions.
 * * @property timeout The maximum duration an action can run before timing out.
 *                  Default is 5 seconds.
 * @property allowConcurrent Whether multiple instances of this action can run simultaneously.
 *                          When false, only one instance will execute at a time.
 *                          Default is false (sequential execution).
 * @property enqueueIfRunning Whether new requests should be queued when the action is already running.
 *                           Only applies when allowConcurrent is false.
 *                           When false, new requests will immediately return ActionResult.AlreadyRunning.
 *                           Default is true (queue new requests).
 */
data class ActionOptions(
    val timeout: Duration = INFINITE,
    val allowConcurrent: Boolean = false,
    val enqueueIfRunning: Boolean = true
)

/**
 * Declares a new typed Action in the current module or app context.
 *
 * Use this form when the action requires input data to run. Actions are isolated * units of behavior that can receive inputs and produce results.
 *
 * Example:
 * ```kotlin
 * // Simple version
 * val setSpeed = action<Double>("setSpeed") { value ->
 *   motor.setSpeed(value)
 *   ActionResult.Success()
 * }
 *
 * // With options
 * val moveRobot = action<Coordinates>("moveRobot", *   options = ActionOptions(
 *     timeout = 10.seconds,
 *     allowConcurrent = true
 *   )
 * ) { coords ->
 *   robot.moveTo(coords)
 *   ActionResult.Success("Moved to $coords")
 * }
 * ```
 *
 * @param name A developer-facing name for diagnostics and traceability
 * @param options Configuration for timeout, concurrency, and queueing behavior
 * @param block The suspendable logic that accepts an input value of type [T] and returns an ActionResult
 * @return An [ActionHandle] representing the declared action
 */
fun <T> action(
    name: String,
    options: ActionOptions = ActionOptions(),
    block: suspend (T) -> ActionResult
): ActionHandle<T> {
    return ActionHandle(
        name = name,
        timeout = options.timeout,
        allowConcurrent = options.allowConcurrent,
        enqueueIfRunning = options.enqueueIfRunning,
        block = block
    )
}

/**
 * Declares a zero-input Action (e.g., a procedure with no parameters) that returns an ActionResult.
 *
 * Use this for actions that don't require input parameters but provide detailed execution results.
 *
 * Example:
 * ```kotlin
 * // Simple version
 * val reboot = action("reboot") {
 *   performSystemReboot()
 *   ActionResult.Success("System rebooted")
 * }
 *
 * // With options
 * val calibrate = action("calibrate",
 *   options = ActionOptions(
 *     timeout = 30.seconds,
 *     allowConcurrent = false,
 *     enqueueIfRunning = false
 *   )
 * ) {
 *   try {
 *     system.calibrate()
 *     ActionResult.Success("Calibration complete")
 *   } catch (e: Exception) {
 *     ActionResult.Failure(ActionError("calibration_failed", e.message ?: "Unknown error"))
 *   }
 * }
 * ```
 *
 * @param name A developer-facing name for diagnostics and traceability
 * @param options Configuration for timeout, concurrency, and queueing behavior
 * @param block The suspendable logic that returns an ActionResult
 * @return An [ActionHandle] representing the declared action
 */
fun action(
    name: String,
    options: ActionOptions = ActionOptions(),
    block: suspend () -> ActionResult
): ActionHandle<Unit> {
    return ActionHandle(
        name = name,
        timeout = options.timeout,
        allowConcurrent = options.allowConcurrent,
        enqueueIfRunning = options.enqueueIfRunning,
        block = { _: Unit -> block() }
    )
}
