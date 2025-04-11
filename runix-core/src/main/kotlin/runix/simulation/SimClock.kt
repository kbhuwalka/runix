package runix.simulation

class SimClock {
    var now: Long = 0L
        private set

    fun advanceTo(time: Long) {
        require(time >= now) { "Cannot go backwards in time" }
        now = time
    }

    fun advanceBy(delta: Long) {
        require(delta >= 0) { "Delta must be non-negative" }
        now += delta
    }
}
