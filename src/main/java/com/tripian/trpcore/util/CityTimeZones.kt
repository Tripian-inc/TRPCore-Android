package com.tripian.trpcore.util

import com.tripian.one.api.cities.model.City
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap

/**
 * Central registry mapping a cityId to its IANA timezone (from [City.timezone])
 * plus helpers to enforce "no past time" checks against the selected city's local
 * clock — not the device clock. Falls back to the device timezone for cities
 * without a registered zone.
 */
object CityTimeZones {

    private val zoneByCityId = ConcurrentHashMap<Int, String>()

    /** Records the timezone of every city that carries one. Safe to call repeatedly. */
    fun register(cities: Iterable<City>?) {
        cities ?: return
        cities.forEach { city ->
            city.timezone?.takeIf { it.isNotBlank() }?.let { zoneByCityId[city.id] = it }
        }
    }

    /** Registered IANA timezone for [cityId], or null when unknown. */
    fun timezoneFor(cityId: Int?): String? = cityId?.let { zoneByCityId[it] }

    private fun zoneOf(timeZoneId: String?): TimeZone =
        timeZoneId?.takeIf { it.isNotBlank() }?.let { TimeZone.getTimeZone(it) }
            ?: TimeZone.getDefault()

    /** "Now" as a [Calendar] in the given timezone (device tz when null/blank). */
    fun nowInZone(timeZoneId: String?): Calendar = Calendar.getInstance(zoneOf(timeZoneId))

    /** "Now" in the timezone registered for [cityId]. */
    fun nowForCity(cityId: Int?): Calendar = nowInZone(timezoneFor(cityId))

    /** y-m-d collapsed to a comparable int (e.g. 2026-06-18 -> 20260618). */
    private fun dayKey(c: Calendar): Int =
        c.get(Calendar.YEAR) * 10000 + c.get(Calendar.MONTH) * 100 + c.get(Calendar.DAY_OF_MONTH)

    /** Trip days are device-local calendar days, so their y-m-d is read in the device tz. */
    private fun dayKeyOf(day: Date): Int =
        dayKey(Calendar.getInstance().apply { time = day })

    /**
     * Minutes-into-day that the user is NOT allowed to select before, for [day] in
     * the city's timezone:
     *  - future day  -> 0      (whole day open)
     *  - the city's "today" -> current minute-of-day (earlier slots are past)
     *  - past day    -> [Int.MAX_VALUE] (the whole day is past)
     */
    fun minSelectableMinutes(day: Date, timeZoneId: String?): Int {
        val now = nowInZone(timeZoneId)
        val dk = dayKeyOf(day)
        val nk = dayKey(now)
        return when {
            dk > nk -> 0
            dk < nk -> Int.MAX_VALUE
            else -> now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        }
    }

    /** Convenience overload resolving the timezone from [cityId]. */
    fun minSelectableMinutes(day: Date, cityId: Int?): Int =
        minSelectableMinutes(day, timezoneFor(cityId))

    /** True when [day] is strictly before the city's current calendar day. */
    fun isDayInPast(day: Date, timeZoneId: String?): Boolean =
        dayKeyOf(day) < dayKey(nowInZone(timeZoneId))

    fun isDayInPast(day: Date, cityId: Int?): Boolean =
        isDayInPast(day, timezoneFor(cityId))

    /**
     * Picker floor as "HH:mm"; null when the whole day is open, "23:59" when fully past.
     * Pickers validate with a strict "> min", so this returns one minute BEFORE the
     * city's "now" to keep "now" itself selectable.
     */
    fun minSelectableTime(day: Date, timeZoneId: String?): String? {
        val minutes = minSelectableMinutes(day, timeZoneId)
        return when {
            minutes <= 0 -> null
            minutes >= 24 * 60 -> "23:59"
            else -> {
                val floor = minutes - 1
                String.format("%02d:%02d", floor / 60, floor % 60)
            }
        }
    }

    fun minSelectableTime(day: Date, cityId: Int?): String? =
        minSelectableTime(day, timezoneFor(cityId))

    /**
     * Earliest selectable "HH:mm" for the time pickers: a future day is left fully
     * open (no floor) so any start time can be picked; today floors at the next
     * half-hour slot in the city's timezone (skipping ahead when fewer than 5
     * minutes remain). Exclusive — the minute before the earliest slot. See
     * [defaultStartTime] for the suggested prefill value.
     */
    fun minSelectableTimeRounded(day: Date, timeZoneId: String?): String? {
        val nowMinutes = minSelectableMinutes(day, timeZoneId)
        if (nowMinutes <= 0) return null
        if (nowMinutes >= 24 * 60) return "23:59"
        val earliest = roundUpToHalfHour(nowMinutes)
        if (earliest >= 24 * 60) return "23:59"
        val floor = (earliest - 1).coerceAtLeast(0)
        return String.format("%02d:%02d", floor / 60, floor % 60)
    }

    fun minSelectableTimeRounded(day: Date, city: City?): String? =
        minSelectableTimeRounded(day, timezoneFor(city?.id) ?: city?.timezone)

    /**
     * Suggested "HH:mm" to prefill a start-time picker with when nothing has been
     * chosen yet: 09:00 for a future day, the next half-hour slot in the city's
     * timezone for today (skipping ahead when fewer than 5 minutes remain).
     * Unlike [minSelectableTimeRounded] this is never a restriction, just a default.
     */
    fun defaultStartTime(day: Date, timeZoneId: String?): String {
        val nowMinutes = minSelectableMinutes(day, timeZoneId)
        val slot = when {
            nowMinutes <= 0 -> 9 * 60
            nowMinutes >= 24 * 60 -> return "23:59"
            else -> roundUpToHalfHour(nowMinutes)
        }
        if (slot >= 24 * 60) return "23:59"
        return String.format("%02d:%02d", slot / 60, slot % 60)
    }

    fun defaultStartTime(day: Date, city: City?): String =
        defaultStartTime(day, timezoneFor(city?.id) ?: city?.timezone)

    private fun roundUpToHalfHour(nowMinutes: Int): Int {
        val remainder = nowMinutes % 30
        var slot = if (remainder == 0) nowMinutes else nowMinutes - remainder + 30
        if (slot - nowMinutes < 5) slot += 30
        return slot
    }

    /**
     * True when the "HH:mm" [timeSlot] on [day] is in the past for the city's clock.
     * Blank/malformed slots are treated as not-past.
     */
    fun isTimeSlotInPast(day: Date, timeSlot: String?, timeZoneId: String?): Boolean {
        val parts = timeSlot?.split(":") ?: return false
        val minutes = runCatching { parts[0].toInt() * 60 + parts[1].toInt() }.getOrNull() ?: return false
        return minutes < minSelectableMinutes(day, timeZoneId)
    }

    fun isTimeSlotInPast(day: Date, timeSlot: String?, cityId: Int?): Boolean =
        isTimeSlotInPast(day, timeSlot, timezoneFor(cityId))
}
