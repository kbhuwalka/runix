package runix.temporal.time

import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration

/**
 * Human-friendly formatting for Duration.
 */
internal fun Duration.formatHuman(): String = when {
    inWholeHours > 0 -> "%dh %dm".format(inWholeHours, (inWholeMinutes % 60))
    inWholeMinutes > 0 -> "%dm %ds".format(inWholeMinutes, (inWholeSeconds % 60))
    inWholeSeconds > 0 -> "%ds".format(inWholeSeconds)
    inWholeMilliseconds > 0 -> "%dms".format(inWholeMilliseconds)
    else -> "<1ms"
}

/**
 * ISO-8601 formatting for Instant.
 */
internal fun Instant.formatIso(): String =
    DateTimeFormatter.ISO_INSTANT.format(this)

/**
 * Compare two ComparableTimeMarks without drift.
 */
internal infix fun ComparableTimeMark.isBefore(other: ComparableTimeMark): Boolean =
    this < other

internal infix fun ComparableTimeMark.isAfter(other: ComparableTimeMark): Boolean =
    this > other

/**
 * Duration between two ComparableTimeMarks.
 */
internal fun ComparableTimeMark.durationSince(other: ComparableTimeMark): Duration =
    this - other
