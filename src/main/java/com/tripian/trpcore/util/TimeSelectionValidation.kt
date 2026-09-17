package com.tripian.trpcore.util

import com.tripian.trpcore.ui.timeline.addplan.MaterialTimePickerHelper

/** Why a picked time is rejected; each maps to one inline warning under its field. */
enum class TimeFieldError(val languageKey: String) {
    START_PASSED(LanguageConst.ADD_PLAN_TIME_START_PASSED),
    END_BEFORE_START(LanguageConst.ADD_PLAN_TIME_END_BEFORE_START)
}

data class TimeSelectionErrors(val start: TimeFieldError? = null, val end: TimeFieldError? = null) {
    val isValid: Boolean get() = start == null && end == null
}

/**
 * Validates a start/end "HH:mm" pair after the fact, so the pickers stay unbounded
 * and the fields explain what is wrong. [earliestStart] is the exclusive floor for
 * the day in the destination's clock (see [CityTimeZones.minSelectableTime]);
 * null means the whole day is open.
 */
object TimeSelectionValidation {

    fun validate(startTime: String?, endTime: String?, earliestStart: String?): TimeSelectionErrors {
        val startError = TimeFieldError.START_PASSED.takeIf { isStartPassed(startTime, earliestStart) }
        val endError = TimeFieldError.END_BEFORE_START.takeIf {
            startTime != null && endTime != null &&
                !MaterialTimePickerHelper.isEndTimeAfterStartTime(startTime, endTime)
        }
        return TimeSelectionErrors(startError, endError)
    }

    private fun isStartPassed(startTime: String?, earliestStart: String?): Boolean {
        val start = minutesOf(startTime) ?: return false
        val floor = minutesOf(earliestStart) ?: return false
        return start <= floor
    }

    private fun minutesOf(time: String?): Int? = time?.split(":")
        ?.takeIf { it.size == 2 }
        ?.let { runCatching { it[0].toInt() * 60 + it[1].toInt() }.getOrNull() }
}
