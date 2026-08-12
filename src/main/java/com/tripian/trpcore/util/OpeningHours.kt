package com.tripian.trpcore.util

import java.util.Calendar
import java.util.Date

/**
 * Parses a POI `hours` string into per-day 24h ranges and answers whether a chosen
 * time span falls inside them.
 *
 * Input looks like `"Sun, Sat: 9:00 AM - 1:00 AM | Mon-Fri: 8:30 AM - 1:00 AM"`;
 * day names may be localized (see [dayNameMappings]). A range whose end is not
 * after its start crosses midnight, so it stays open until that hour the next day.
 */
object OpeningHours {

    val DAY_ORDER = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    data class Range(val startMinutes: Int, val endMinutes: Int) {
        val crossesMidnight: Boolean get() = endMinutes <= startMinutes
    }

    /**
     * Multi-language day name mappings to English abbreviations.
     * Supports: English, Spanish, German, French, Turkish, Italian, Portuguese
     */
    private val dayNameMappings = mapOf(
        "Mon" to "Mon", "Tue" to "Tue", "Wed" to "Wed", "Thu" to "Thu", "Fri" to "Fri", "Sat" to "Sat", "Sun" to "Sun",
        "Monday" to "Mon", "Tuesday" to "Tue", "Wednesday" to "Wed", "Thursday" to "Thu", "Friday" to "Fri", "Saturday" to "Sat", "Sunday" to "Sun",
        "Lun" to "Mon", "Mar" to "Tue", "Mié" to "Wed", "Mie" to "Wed", "Jue" to "Thu", "Vie" to "Fri", "Sáb" to "Sat", "Sab" to "Sat", "Dom" to "Sun",
        "Lunes" to "Mon", "Martes" to "Tue", "Miércoles" to "Wed", "Miercoles" to "Wed", "Jueves" to "Thu", "Viernes" to "Fri", "Sábado" to "Sat", "Sabado" to "Sat", "Domingo" to "Sun",
        "Mo" to "Mon", "Di" to "Tue", "Mi" to "Wed", "Do" to "Thu", "Fr" to "Fri", "Sa" to "Sat", "So" to "Sun",
        "Montag" to "Mon", "Dienstag" to "Tue", "Mittwoch" to "Wed", "Donnerstag" to "Thu", "Freitag" to "Fri", "Samstag" to "Sat", "Sonntag" to "Sun",
        "Mer" to "Wed", "Jeu" to "Thu", "Ven" to "Fri", "Sam" to "Sat", "Dim" to "Sun",
        "Lundi" to "Mon", "Mardi" to "Tue", "Mercredi" to "Wed", "Jeudi" to "Thu", "Vendredi" to "Fri", "Samedi" to "Sat", "Dimanche" to "Sun",
        "Pzt" to "Mon", "Sal" to "Tue", "Çar" to "Wed", "Car" to "Wed", "Per" to "Thu", "Cum" to "Fri", "Cmt" to "Sat", "Paz" to "Sun",
        "Pazartesi" to "Mon", "Salı" to "Tue", "Sali" to "Tue", "Çarşamba" to "Wed", "Carsamba" to "Wed", "Perşembe" to "Thu", "Persembe" to "Thu", "Cuma" to "Fri", "Cumartesi" to "Sat", "Pazar" to "Sun",
        "Gio" to "Thu",
        "Lunedì" to "Mon", "Lunedi" to "Mon", "Martedì" to "Tue", "Martedi" to "Tue", "Mercoledì" to "Wed", "Mercoledi" to "Wed", "Giovedì" to "Thu", "Giovedi" to "Thu", "Venerdì" to "Fri", "Venerdi" to "Fri", "Sabato" to "Sat", "Domenica" to "Sun",
        "Seg" to "Mon", "Ter" to "Tue", "Qua" to "Wed", "Qui" to "Thu", "Sex" to "Fri",
        "Segunda" to "Mon", "Terça" to "Tue", "Terca" to "Tue", "Quarta" to "Wed", "Quinta" to "Thu", "Sexta" to "Fri"
    )

    private val allDayNames: List<String> by lazy {
        dayNameMappings.keys.sortedByDescending { it.length }
    }

    /** English day abbreviation ("Mon".."Sun") for [date], or null when it can't be derived. */
    fun dayKeyOf(date: Date?): String? {
        if (date == null) return null
        val calendar = Calendar.getInstance().apply { time = date }
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Mon"
            Calendar.TUESDAY -> "Tue"
            Calendar.WEDNESDAY -> "Wed"
            Calendar.THURSDAY -> "Thu"
            Calendar.FRIDAY -> "Fri"
            Calendar.SATURDAY -> "Sat"
            Calendar.SUNDAY -> "Sun"
            else -> null
        }
    }

    /** English day abbreviation to `"HH:mm - HH:mm"`; days with no entry are absent. */
    fun dayTexts(hoursString: String?): Map<String, String> {
        if (hoursString.isNullOrBlank()) return emptyMap()

        val dayHoursMap = mutableMapOf<String, String>()
        for (group in hoursString.split("|").map { it.trim() }) {
            if (!group.contains(":")) continue

            val daysPart = findDaysPart(group)
            if (daysPart.isEmpty()) continue

            val timePart = group.substring(daysPart.length).trim().removePrefix(":").trim()
            val convertedTime = convertTo24HourFormat(timePart)

            for (day in parseDays(daysPart)) {
                dayHoursMap[day] = convertedTime
            }
        }
        return dayHoursMap
    }

    /** `"HH:mm - HH:mm"` for [date]'s weekday, or null when that day has no entry. */
    fun dayText(hoursString: String?, date: Date?): String? {
        val dayKey = dayKeyOf(date) ?: return null
        return dayTexts(hoursString)[dayKey]
    }

    /**
     * Whether `[startTime, endTime]` on [date] fits inside that day's opening hours.
     * Returns null when it cannot be decided — no hours data, an unparsable string,
     * or an incomplete selection — so callers can stay silent instead of warning.
     */
    fun coversSelection(
        hoursString: String?,
        date: Date?,
        startTime: String?,
        endTime: String?
    ): Boolean? {
        val selectionStart = toMinutes(startTime) ?: return null
        val selectionEnd = toMinutes(endTime) ?: return null
        val dayKey = dayKeyOf(date) ?: return null

        val dayTexts = dayTexts(hoursString)
        if (dayTexts.isEmpty()) return null

        val range = dayTexts[dayKey]?.let { parseRange(it) } ?: return false

        return if (range.crossesMidnight) {
            selectionStart >= range.startMinutes || selectionEnd <= range.endMinutes
        } else {
            selectionStart >= range.startMinutes && selectionEnd <= range.endMinutes
        }
    }

    private fun parseRange(text: String): Range? {
        val parts = text.split("-").map { it.trim() }
        if (parts.size != 2) return null
        val start = toMinutes(parts[0]) ?: return null
        val end = toMinutes(parts[1]) ?: return null
        return Range(start, end)
    }

    private fun toMinutes(time: String?): Int? {
        val parts = time?.split(":") ?: return null
        if (parts.size != 2) return null
        val hour = parts[0].trim().toIntOrNull() ?: return null
        val minute = parts[1].trim().toIntOrNull() ?: return null
        return hour * 60 + minute
    }

    private fun findDaysPart(group: String): String {
        var lastDayEnd = 0
        for (i in group.indices) {
            for (dayName in allDayNames) {
                if (group.startsWith(dayName, i, ignoreCase = true)) {
                    val endPos = i + dayName.length
                    if (endPos > lastDayEnd) lastDayEnd = endPos
                }
            }
        }
        return group.substring(0, lastDayEnd)
    }

    private fun normalizeDayName(localizedDay: String): String? {
        val trimmed = localizedDay.trim()
        for ((key, value) in dayNameMappings) {
            if (key.equals(trimmed, ignoreCase = true)) return value
        }
        return null
    }

    private fun parseDays(daysString: String): List<String> {
        val result = mutableListOf<String>()

        for (part in daysString.split(",").map { it.trim() }) {
            if (part.contains("-")) {
                val rangeParts = part.split("-").map { it.trim() }
                if (rangeParts.size != 2) continue

                val startDay = normalizeDayName(rangeParts[0]) ?: continue
                val endDay = normalizeDayName(rangeParts[1]) ?: continue
                val startIdx = DAY_ORDER.indexOf(startDay)
                val endIdx = DAY_ORDER.indexOf(endDay)
                if (startIdx < 0 || endIdx < 0) continue

                if (startIdx <= endIdx) {
                    for (i in startIdx..endIdx) result.add(DAY_ORDER[i])
                } else {
                    for (i in startIdx until DAY_ORDER.size) result.add(DAY_ORDER[i])
                    for (i in 0..endIdx) result.add(DAY_ORDER[i])
                }
            } else {
                normalizeDayName(part)
                    ?.takeIf { DAY_ORDER.contains(it) }
                    ?.let { result.add(it) }
            }
        }

        return result
    }

    private fun convertTo24HourFormat(timeString: String): String {
        val parts = timeString.split("-").map { it.trim() }
        if (parts.size != 2) return timeString
        return "${convert12To24(parts[0])} - ${convert12To24(parts[1])}"
    }

    private fun convert12To24(time: String): String {
        val trimmed = time.trim().uppercase()
        val isPM = trimmed.contains("PM")
        val isAM = trimmed.contains("AM")

        val timeOnly = trimmed.replace("AM", "").replace("PM", "").trim()
        val timeParts = timeOnly.split(":").map { it.trim() }
        if (timeParts.size != 2) return time

        var hour = timeParts[0].toIntOrNull() ?: return time
        val minute = timeParts[1].toIntOrNull() ?: return time

        if (isPM && hour != 12) {
            hour += 12
        } else if (isAM && hour == 12) {
            hour = 0
        }

        return String.format("%02d:%02d", hour, minute)
    }
}
