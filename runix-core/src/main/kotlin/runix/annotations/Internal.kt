package runix.annotations

/**
 * Marks a public API as internal to the Runix framework.
 *
 * This indicates that the type is visible for technical reasons
 * (e.g. subclassing, framework composition), but is not designed
 * for external implementation or general use.
 *
 * External developers should not rely on these APIs unless explicitly instructed.
 *
 * Framework-internal users may opt-in:
 * ```
 * @OptIn(Internal::class)
 * ```
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This API is internal to Runix and not stable for public use."
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPEALIAS
)
annotation class Internal