package runix.core.logging.memory

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.TimeSource

class NumericTracker(flow: Flow<Double>) {
    private val samples = mutableListOf<Pair<Long, Double>>() // (timestamp, value)
    private val clock = TimeSource.Monotonic

    init {
        CoroutineScope(Dispatchers.Default).launch {
            flow.collect { value ->
                val now = clock.markNow().elapsedNow().inWholeMilliseconds
                samples.add(now to value)
            }
        }
    }

    fun average(window: Duration): Double {
        val now = clock.markNow().elapsedNow().inWholeMilliseconds
        val cutoff = now - window.inWholeMilliseconds
        samples.removeIf { it.first < cutoff }
        val windowSamples = samples.filter { it.first >= cutoff }.map { it.second }
        return if (windowSamples.isNotEmpty()) windowSamples.average() else 0.0
    }

    fun variance(window: Duration): Double {
        val now = clock.markNow().elapsedNow().inWholeMilliseconds
        val cutoff = now - window.inWholeMilliseconds
        samples.removeIf { it.first < cutoff }
        val windowSamples = samples.filter { it.first >= cutoff }.map { it.second }
        val avg = windowSamples.average()
        return if (windowSamples.isNotEmpty()) {
            windowSamples.map { (it - avg).pow(2) }.average()
        } else 0.0
    }
}