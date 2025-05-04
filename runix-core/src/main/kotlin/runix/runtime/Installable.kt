package runix.runtime

import runix.annotations.Internal

/**
 * Represents a self-contained unit of functionality that can be installed
 * into an [App] at startup.
 *
 * This is the extension point for both:
 * - Behavior modules (via [Module], which defines start/stop logic)
 * - Infrastructure components (e.g. telemetry, signal registration)
 */
@Internal
interface Installable {
    /**
     * Called during [App.start] before any lifecycle hooks or module execution.
     */
    fun install(app: App)
}
