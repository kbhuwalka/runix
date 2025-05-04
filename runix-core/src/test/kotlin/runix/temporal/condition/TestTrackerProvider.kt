package runix.temporal.condition

import runix.temporal.trackers.BooleanTracker
import runix.temporal.trackers.CategoricalTracker
import runix.temporal.trackers.NumericTracker
import runix.temporal.trackers.TrackerProvider

internal class TestTrackerProvider(
    val bool: BooleanTracker? = null,
    val numeric: NumericTracker? = null,
    val categorical: CategoricalTracker<*>? = null
) : TrackerProvider {

    override fun getBooleanTracker(key: String): BooleanTracker =
        bool ?: error("BooleanTracker not provided for key: $key")

    override fun getNumericTracker(key: String): NumericTracker =
        numeric ?: error("NumericTracker not provided for key: $key")

    override fun getCategoricalTracker(key: String): CategoricalTracker<*> =
        categorical ?: error("CategoricalTracker not provided for key: $key")
}
