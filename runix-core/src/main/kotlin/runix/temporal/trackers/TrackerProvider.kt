package runix.temporal.trackers

internal interface TrackerProvider {
    fun getBooleanTracker(key: String): BooleanTracker
    fun getNumericTracker(key: String): NumericTracker
    fun getCategoricalTracker(key: String): CategoricalTracker<*>
}
