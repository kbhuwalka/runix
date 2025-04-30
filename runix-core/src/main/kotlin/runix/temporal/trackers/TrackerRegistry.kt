package runix.temporal.trackers

import java.util.concurrent.ConcurrentHashMap

/**
 * Runtime registry of signal trackers, keyed by unique signal ID.
 *
 * This replaces the old TemporalEngine object. It is now responsible only
 * for storing and returning tracker instances by key — not creating them.
 *
 * Trackers are registered externally at monitor compile time.
 */
internal object TrackerRegistry : TrackerProvider {

    private val booleanTrackers = ConcurrentHashMap<String, BooleanTracker>()
    private val numericTrackers = ConcurrentHashMap<String, NumericTracker>()
    private val categoricalTrackers = ConcurrentHashMap<String, CategoricalTracker<*>>()

    // --- TrackerProvider implementation ---

    override fun getBooleanTracker(key: String): BooleanTracker =
        booleanTrackers[key] ?: error("BooleanTracker not registered for key: $key")

    override fun getNumericTracker(key: String): NumericTracker =
        numericTrackers[key] ?: error("NumericTracker not registered for key: $key")

    override fun getCategoricalTracker(key: String): CategoricalTracker<*> =
        categoricalTrackers[key] ?: error("CategoricalTracker not registered for key: $key")

    // --- Registration API ---

    fun register(key: String, tracker: BooleanTracker) {
        booleanTrackers[key] = tracker
    }

    fun register(key: String, tracker: NumericTracker) {
        numericTrackers[key] = tracker
    }

    fun register(key: String, tracker: CategoricalTracker<*>) {
        categoricalTrackers[key] = tracker
    }

    fun unregister(key: String) {
        when {
            booleanTrackers.containsKey(key) -> {
                val tracker = booleanTrackers.remove(key)
                tracker?.stop()
                tracker?.clearHistory()
            }

            numericTrackers.containsKey(key) -> {
                val tracker = numericTrackers.remove(key)
                tracker?.stop()
                tracker?.clearHistory()
            }

            categoricalTrackers.containsKey(key) -> {
                val tracker = categoricalTrackers.remove(key)
                tracker?.stop()
                tracker?.clearHistory()
            }

            else -> {
                // Missing tracker
            }
        }
    }
}