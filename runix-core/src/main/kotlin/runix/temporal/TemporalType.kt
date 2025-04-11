package runix.temporal

import kotlin.time.Duration

sealed class TemporalType {
    object Instant : TemporalType()
    data class Persisted(val duration: Duration) : TemporalType()
    data class WasStable(val duration: Duration) : TemporalType()
}
