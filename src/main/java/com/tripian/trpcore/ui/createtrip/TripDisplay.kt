package com.tripian.trpcore.ui.createtrip

import com.tripian.one.api.timeline.model.Timeline
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Display + filtering helpers for the "My Trips" list ([ACMyTrips]) built from the
 * user's existing [Timeline]s. A timeline has no trip-level name, so the card
 * title is the plan cities joined and the subtitle is the country.
 *
 * Dates come from the timeline's plans (each plan carries its own start/end).
 * minSdk is 24 (< 26), so this uses java.util.Calendar / SimpleDateFormat rather
 * than java.time. Plan date strings are expected to start with "yyyy-MM-dd".
 */
internal object TripDisplay {

    private val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val display = SimpleDateFormat("MM/dd/yyyy", Locale.US)
    private const val DAY_MS = 24L * 60L * 60L * 1000L

    private fun plans(t: Timeline) = t.plans.orEmpty()

    private fun parseMillis(raw: String?): Long? {
        val s = raw?.trim()?.takeIf { it.length >= 10 } ?: return null
        return runCatching { parser.parse(s.take(10))?.time }.getOrNull()
    }

    private fun todayStartMillis(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    // The list response can be light: plan start/end may be absent while the
    // profile segments still carry them (or vice-versa). Scan BOTH so the date
    // row populates whenever the timeline has any dated piece.
    private fun startCandidates(t: Timeline): List<Long> =
        plans(t).mapNotNull { parseMillis(it.startDate) } +
            (t.tripProfile?.segments?.mapNotNull { parseMillis(it.startDate) } ?: emptyList())

    private fun endCandidates(t: Timeline): List<Long> =
        plans(t).mapNotNull { parseMillis(it.endDate) } +
            (t.tripProfile?.segments?.mapNotNull { parseMillis(it.endDate) } ?: emptyList())

    fun earliestStart(t: Timeline): Long? = startCandidates(t).minOrNull()

    fun latestEnd(t: Timeline): Long? = endCandidates(t).maxOrNull()

    /**
     * Keep timelines whose latest plan end date is today or later (ongoing trips
     * stay listed). Undated timelines are kept — better to show a real trip than
     * silently drop it when the list endpoint returns sparse plan dates.
     */
    fun notPast(list: List<Timeline>): List<Timeline> {
        val today = todayStartMillis()
        return list.filter { t ->
            val end = latestEnd(t) ?: earliestStart(t)
            end == null || end >= today
        }
    }

    /** Sort ascending by earliest start (undated last). */
    fun sortByStart(list: List<Timeline>): List<Timeline> =
        list.sortedBy { earliestStart(it) ?: Long.MAX_VALUE }

    /** Whole days from today to the trip start; null when started/ongoing/undated. */
    fun daysUntil(t: Timeline): Int? {
        val start = earliestStart(t) ?: return null
        val today = todayStartMillis()
        if (start <= today) return null
        return ((start - today) / DAY_MS).toInt()
    }

    fun dateRange(t: Timeline): String {
        val s = earliestStart(t)?.let { display.format(Date(it)) }
        val e = latestEnd(t)?.let { display.format(Date(it)) }
        return when {
            s != null && e != null -> "$s - $e"
            s != null -> s
            else -> ""
        }
    }

    /** Title = distinct plan-city names joined; falls back to the timeline city. */
    fun cityTitle(t: Timeline): String {
        val fromPlans = plans(t).mapNotNull { it.city?.name?.takeIf { n -> n.isNotBlank() } }.distinct()
        return if (fromPlans.isNotEmpty()) fromPlans.joinToString(", ") else t.city?.name.orEmpty()
    }

    /** Subtitle = distinct plan-city countries joined; falls back to the timeline country. */
    fun countrySubtitle(t: Timeline): String {
        val countries = plans(t).mapNotNull { it.city?.country?.name?.takeIf { n -> n.isNotBlank() } }.distinct()
        return if (countries.isNotEmpty()) countries.joinToString(", ") else t.city?.country?.name.orEmpty()
    }

    fun imageUrl(t: Timeline): String? =
        t.city?.image?.url ?: plans(t).firstNotNullOfOrNull { it.city?.image?.url }
}
