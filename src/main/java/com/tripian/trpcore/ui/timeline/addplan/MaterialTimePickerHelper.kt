package com.tripian.trpcore.ui.timeline.addplan

import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.tripian.trpcore.R

/**
 * Helper for MaterialTimePicker creation, time conversions and validation
 * Supports 12-hour format (AM/PM) display with 24-hour internal storage
 */
object MaterialTimePickerHelper {

    /**
     * Create MaterialTimePicker with 12h format and custom theme
     * @param initialTime Initial time in HH:mm format (24h), nullable
     * @return MaterialTimePicker configured for 12-hour display
     */
    fun createTimePicker(initialTime: String?): MaterialTimePicker {
        // Parse initial time or use defaults
        val (hour24, minute) = if (initialTime != null) {
            parseTime24h(initialTime)
        } else {
            // Default: 10:00 AM
            Pair(10, 0)
        }

        return MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(hour24)
            .setMinute(minute)
            .setTheme(R.style.TrpCustomTimePickerTheme)
            .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)  // Clock-only mode
            .build()
    }

    /**
     * Parse HH:mm (24h) to hour and minute
     * @param time Time string in HH:mm format (e.g., "14:30")
     * @return Pair of (hour, minute). Returns (10, 0) if parsing fails.
     */
    fun parseTime24h(time: String): Pair<Int, Int> {
        val parts = time.split(":")
        if (parts.size != 2) return Pair(10, 0)
        val hour = parts[0].toIntOrNull() ?: 10
        val minute = parts[1].toIntOrNull() ?: 0
        return Pair(hour, minute)
    }

    /**
     * Format hour/minute to HH:mm (24h storage format)
     * @param hour Hour (0-23)
     * @param minute Minute (0-59)
     * @return Time string in HH:mm format (e.g., "14:30")
     */
    fun formatTo24h(hour: Int, minute: Int): String {
        return String.format("%02d:%02d", hour, minute)
    }

    /**
     * Convert HH:mm (24h) to h:mm AM/PM (12h display)
     * @param time24 Time string in HH:mm format (e.g., "14:30")
     * @return Time string in h:mm AM/PM format (e.g., "2:30 PM")
     */
    fun formatTo12h(time24: String): String {
        val (hour24, minute) = parseTime24h(time24)

        val isPM = hour24 >= 12
        val hour12 = when {
            hour24 == 0 -> 12  // Midnight: 00:00 → 12:00 AM
            hour24 > 12 -> hour24 - 12  // PM hours
            else -> hour24  // AM hours
        }

        val amPm = if (isPM) "PM" else "AM"
        return String.format("%d:%02d %s", hour12, minute, amPm)
    }

    /**
     * Validate end time > start time
     * @param startTime Start time in HH:mm format
     * @param endTime End time in HH:mm format
     * @return true if end time is strictly after start time, false otherwise
     */
    fun isEndTimeAfterStartTime(startTime: String?, endTime: String?): Boolean {
        if (startTime == null || endTime == null) return false

        val startMinutes = timeToMinutes(startTime)
        val endMinutes = timeToMinutes(endTime)

        return endMinutes > startMinutes
    }

    /**
     * Convert time string to total minutes for comparison
     * @param time Time string in HH:mm format
     * @return Total minutes (hour * 60 + minute)
     */
    private fun timeToMinutes(time: String): Int {
        val (hour, minute) = parseTime24h(time)
        return hour * 60 + minute
    }
}
