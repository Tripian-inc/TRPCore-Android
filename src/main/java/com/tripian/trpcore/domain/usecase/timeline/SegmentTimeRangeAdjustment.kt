package com.tripian.trpcore.domain.usecase.timeline

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** A time window as naive local `yyyy-MM-dd HH:mm[:ss]` strings. */
internal data class SegmentTimeRange(val startDate: String, val endDate: String)

/**
 * Keeps a step inside its segment's [startDate, endDate] window. The backend
 * rejects a step whose time falls outside that window, so the segment has to be
 * widened before the step is moved.
 */
internal object SegmentTimeRangeAdjustment {

    private const val DATE_LENGTH = 10
    private const val DATE_PATTERN = "yyyy-MM-dd"
    private val DATE_REGEX = Regex("""\d{4}-\d{2}-\d{2}""")
    private val TIME_REGEX = Regex("""\d{2}:\d{2}(:\d{2})?""")

    /**
     * Returns the segment window widened to `(min(segmentStart, stepStart), max(segmentEnd, stepEnd))`,
     * or null when the step already fits or an input is not a parseable datetime.
     * A null [segmentEnd] is treated as [segmentStart]. Every widened bound keeps the
     * time format (with or without seconds) of the segment bound it replaces.
     */
    fun widenedRange(
        segmentStart: String?,
        segmentEnd: String?,
        stepStart: String,
        stepEnd: String
    ): SegmentTimeRange? {
        val currentStart = segmentStart ?: return null
        val currentEnd = segmentEnd ?: currentStart
        val currentStartKey = sortKey(currentStart) ?: return null
        val currentEndKey = sortKey(currentEnd) ?: return null
        val stepStartKey = sortKey(stepStart) ?: return null
        val stepEndKey = sortKey(stepEnd) ?: return null

        val widenStart = stepStartKey < currentStartKey
        val widenEnd = stepEndKey > currentEndKey
        if (!widenStart && !widenEnd) return null

        return SegmentTimeRange(
            startDate = if (widenStart) formatLike(currentStart, stepStartKey) else currentStart,
            endDate = if (widenEnd) formatLike(currentEnd, stepEndKey) else currentEnd
        )
    }

    /**
     * Builds a step's window on [date] (`yyyy-MM-dd`) from picker times (`HH:mm[:ss]`).
     * A null [endTime] collapses the window to [startTime]; an [endTime] earlier than
     * [startTime] is placed on the following day. Returns null on unparseable input.
     */
    fun stepWindow(date: String, startTime: String, endTime: String?): SegmentTimeRange? {
        val day = datePartOf(date) ?: return null
        val start = normalizedTime(startTime) ?: return null
        val resolvedEndTime = endTime ?: startTime
        val end = normalizedTime(resolvedEndTime) ?: return null
        val endDay = if (end < start) nextDay(day) ?: return null else day
        return SegmentTimeRange(
            startDate = "$day $startTime",
            endDate = "$endDay $resolvedEndTime"
        )
    }

    /** `yyyy-MM-dd HH:mm[:ss]` → `yyyy-MM-dd HH:mm:ss`, comparable as a string; null on parse miss. */
    private fun sortKey(raw: String): String? {
        val day = datePartOf(raw) ?: return null
        val time = timePartOf(raw)?.let(::normalizedTime) ?: return null
        return "$day $time"
    }

    private fun normalizedTime(time: String): String? {
        if (!TIME_REGEX.matches(time)) return null
        return if (time.length == 5) "$time:00" else time
    }

    private fun formatLike(template: String, sortKey: String): String {
        val templateHasSeconds = (timePartOf(template)?.length ?: 0) > 5
        return if (templateHasSeconds) sortKey else sortKey.substring(0, DATE_LENGTH + 6)
    }

    private fun nextDay(day: String): String? {
        val format = SimpleDateFormat(DATE_PATTERN, Locale.US).apply { isLenient = false }
        val parsed = try {
            format.parse(day)
        } catch (_: ParseException) {
            null
        } ?: return null
        val calendar = Calendar.getInstance().apply {
            time = parsed
            add(Calendar.DAY_OF_MONTH, 1)
        }
        return format.format(calendar.time)
    }

    /** `yyyy-MM-dd HH:mm[:ss]` or `yyyy-MM-dd` → `yyyy-MM-dd`; null on parse miss. */
    fun datePartOf(raw: String?): String? {
        if (raw.isNullOrBlank() || raw.length < DATE_LENGTH) return null
        val datePiece = raw.substring(0, DATE_LENGTH)
        return datePiece.takeIf { DATE_REGEX.matches(it) }
    }

    /** `yyyy-MM-dd HH:mm[:ss]` → `HH:mm[:ss]`; null when there is no time part. */
    fun timePartOf(raw: String?): String? {
        if (raw.isNullOrBlank() || raw.length <= DATE_LENGTH) return null
        val separator = raw[DATE_LENGTH]
        if (separator != ' ' && separator != 'T') return null
        return raw.substring(DATE_LENGTH + 1).takeIf { TIME_REGEX.matches(it) }
    }
}
