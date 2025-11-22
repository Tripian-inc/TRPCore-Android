package com.tripian.trpcore.util.extensions

import android.annotation.SuppressLint
import android.text.TextUtils
import com.tripian.trpcore.domain.model.OpenHour
import com.tripian.trpcore.util.extensions.DayOfWeek.values
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Created by semihozkoroglu on 19.08.2020.
 */
fun formatDate2String(
    dateText: String?,
    inputFormat: String = "yyyy-MM-dd hh:mm:ss",
    outputformat: String = "yyyy-MM-dd"
): String {
    val date: Date?

    if (!dateText.isNullOrEmpty()) {
        try {
            var sdf = SimpleDateFormat(inputFormat, Locale.forLanguageTag(appLanguage))

            date = sdf.parse(dateText)

            sdf = SimpleDateFormat(outputformat, Locale.forLanguageTag(appLanguage))

            return sdf.format(date)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    return ""
}

@SuppressLint("SimpleDateFormat")
fun formatDate2Timestamp(dateText: String?, inputFormat: String = "yyyy-MM-dd hh:mm:ss"): Long {
    val sdf: SimpleDateFormat?
    val date: Date?

    if (!dateText.isNullOrEmpty()) {
        try {
            sdf = SimpleDateFormat(inputFormat, Locale.forLanguageTag(appLanguage))

            date = sdf.parse(dateText)

            return date.time
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    return 0L
}

fun getDate(timestamp: Long): String {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp

    val sdf: SimpleDateFormat?

    try {
        sdf = SimpleDateFormat("yyyy-MM-dd", Locale.forLanguageTag(appLanguage))

        return sdf.format(calendar.time)
    } catch (e: ParseException) {
        e.printStackTrace()
    }

    return ""
}

fun Long.getYear(): String {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = this

    val sdf: SimpleDateFormat?

    try {
        sdf = SimpleDateFormat("yyyy", Locale.forLanguageTag(appLanguage))

        return sdf.format(calendar.time)
    } catch (e: ParseException) {
        e.printStackTrace()
    }

    return ""
}

fun Long.getMonthName(): String {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = this

    val sdf: SimpleDateFormat?

    try {
        val locale = Locale.forLanguageTag(appLanguage)
        sdf = SimpleDateFormat("MMM", locale)

        return sdf.format(calendar.time)
    } catch (e: ParseException) {
        e.printStackTrace()
    }

    return ""
}

fun Long.getDay(): String {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = this

    val sdf: SimpleDateFormat?

    try {
        val locale = Locale.forLanguageTag(appLanguage)
        sdf = SimpleDateFormat("dd", locale)

        return sdf.format(calendar.time)
    } catch (e: ParseException) {
        e.printStackTrace()
    }

    return ""
}

fun String?.getMonthAndDay(): String? {
    return this?.formatDateToFormat(formatTo = "dd.MM")
}

private fun String.formatDateToFormat(
    defaultFormat: String = "yyyy-MM-dd",
    formatTo: String
): String? {
    return try {
        val locale = Locale.forLanguageTag(appLanguage)
        val defaultDateFormat: DateFormat = SimpleDateFormat(defaultFormat, Locale.forLanguageTag(appLanguage))
        val dateFormatTo: DateFormat = SimpleDateFormat(formatTo, locale)

        val date = defaultDateFormat.parse(this)
        dateFormatTo.format(date ?: Date())
    } catch (e: ParseException) {
        this
    }
}

fun String.formatDate(): String? {
    val locale = Locale.forLanguageTag(appLanguage)
    val format: DateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.forLanguageTag(appLanguage))
    val formatTo: DateFormat = SimpleDateFormat("MMM d, yyyy", locale)
    var date: Date? = null
    try {
        date = format.parse(this)
    } catch (e: ParseException) {
//        TRPLogger.debug(e.message)
    }
    return formatTo.format(date)
}

fun String.formatDateWithShortName(): String? {
    val locale = Locale.forLanguageTag(appLanguage)
    val format: DateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.forLanguageTag(appLanguage))
    val formatTo: DateFormat = SimpleDateFormat("dd MMM yyyy", locale)
    var date: Date? = null
    try {
        date = format.parse(this)
    } catch (e: ParseException) {
//        TRPLogger.debug(e.message)
    }
    return formatTo.format(date)
}

fun String.formatDateDay(): String? {
    val locale = Locale.forLanguageTag(appLanguage)
    val format: DateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.forLanguageTag(appLanguage))
    val formatTo: DateFormat = SimpleDateFormat("MMM d", locale)
    var date: Date? = null
    try {
        date = format.parse(this)
    } catch (e: ParseException) {
//        TRPLogger.debug(e.message)
    }
    return formatTo.format(date)
}

fun String.formatDateDayShortName(): String? {
    val locale = Locale.forLanguageTag(appLanguage)
    val format: DateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.forLanguageTag(appLanguage))
    val formatTo: DateFormat = SimpleDateFormat("EEE", locale)
    var date: Date? = null
    try {
        date = format.parse(this)
    } catch (e: ParseException) {
//        TRPLogger.debug(e.message)
    }
    return formatTo.format(date)
}

fun today(): String {
    val format: DateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.forLanguageTag(appLanguage))

    return format.format(Date())
}

fun afterToday(): String {
    val format: DateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.forLanguageTag(appLanguage))

    val cal = Calendar.getInstance()
    cal.time = Date()
    cal.add(Calendar.DATE, -1)

    return format.format(cal.time)
}

fun Long.formatDate(format: String = "MMM dd, yyyy"): String {
    val date = if (this == -1L) {
        System.currentTimeMillis()
    } else {
        this
    }
    val locale = Locale.forLanguageTag(appLanguage)
    val newFormat = SimpleDateFormat("MMM dd, yyyy", locale)
//    newFormat.timeZone = TimeZone.getTimeZone("UTC")
    return newFormat.format(Date(date))
}

fun Long.formatTime(): String {
    val date = if (this == -1L) {
        "21:00".hourToMillis()
    } else {
        this
    }

    val newFormat = SimpleDateFormat("HH:mm")
    return newFormat.format(Date(date))
}

fun Long.formatTime24(): String {
    val date = if (this == -1L) {
        "21:00".hourToMillis()
    } else {
        this
    }

    val newFormat = SimpleDateFormat("HH:mm")
    return newFormat.format(Date(date))
}

fun String.hourToMillis(): Long {
    val hourArr = this.split(":").toTypedArray()
    val hourAsInt = hourArr[0].toInt()
    val minuteAsInt = hourArr[1].toInt()
    val calendar = Calendar.getInstance()
    calendar[Calendar.HOUR_OF_DAY] = hourAsInt
    calendar[Calendar.MINUTE] = minuteAsInt
    calendar[Calendar.AM_PM] = calendar[Calendar.AM_PM]
    return calendar.timeInMillis
}

fun getDiffDate(date2: Long, date1: Long): Long {
    val calendar1 = Calendar.getInstance()
    calendar1.timeInMillis = date1
    calendar1[Calendar.HOUR_OF_DAY] = 0
    calendar1[Calendar.MINUTE] = 0
    calendar1[Calendar.SECOND] = 0
    calendar1[Calendar.MILLISECOND] = 0
    val calendar2 = Calendar.getInstance()
    calendar2.timeInMillis = date2
    calendar2[Calendar.HOUR_OF_DAY] = 0
    calendar2[Calendar.MINUTE] = 0
    calendar2[Calendar.SECOND] = 0
    calendar2[Calendar.MILLISECOND] = 0
    val diff = calendar2.time.time - calendar1.time.time
    return if (diff > 0) {
        TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)
    } else {
        -1
    }
}

//fun String?.numberOfDaysBetween(toDate: String): String {
//    val calToday = Calendar.getInstance()
//    calToday.timeInMillis = this?.parseDate() ?: 0
//    calToday[Calendar.HOUR_OF_DAY] = 0
//    calToday[Calendar.MINUTE] = 0
//    calToday[Calendar.SECOND] = 0
//    calToday[Calendar.MILLISECOND] = 0
//
//    val calDepart = Calendar.getInstance()
//    calDepart.timeInMillis = toDate.parseDate()
//    calDepart[Calendar.HOUR_OF_DAY] = 0
//    calDepart[Calendar.MINUTE] = 0
//    calDepart[Calendar.SECOND] = 0
//    calDepart[Calendar.MILLISECOND] = 0
//
//    val diff = calDepart.time.time - calToday.time.time
//    val days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) + 1
//    return if (days > 1) {
//        (days).toString() + " days"
//    } else {
//        "1 day"
//    }
//}

fun String?.numberOfDaysBetweenCount(toDate: String): Int {
    val calToday = Calendar.getInstance()
    calToday.timeInMillis = this?.parseDate() ?: 0
    calToday[Calendar.HOUR_OF_DAY] = 0
    calToday[Calendar.MINUTE] = 0
    calToday[Calendar.SECOND] = 0
    calToday[Calendar.MILLISECOND] = 0

    val calDepart = Calendar.getInstance()
    calDepart.timeInMillis = toDate.parseDate()
    calDepart[Calendar.HOUR_OF_DAY] = 0
    calDepart[Calendar.MINUTE] = 0
    calDepart[Calendar.SECOND] = 0
    calDepart[Calendar.MILLISECOND] = 0

    val diff = calDepart.time.time - calToday.time.time
    val days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) + 1
    return days.toInt()
}

fun String.parseDate(): Long {
    val dates: List<String>? = removeLastCharacter(this)?.split("T")

    if (dates != null && dates.size > 1) {
        val dateArr: Array<String> = dates[0].split("-").toTypedArray()
        val year = Integer.valueOf(dateArr[0])
        //month − Value to be used for MONTH field. 0 is January
        //month − Value to be used for MONTH field. 0 is January
        val monthOfYear = Integer.valueOf(dateArr[1]) - 1
        val dayOfMonth = Integer.valueOf(dateArr[2])

        val calendar = Calendar.getInstance(Locale.forLanguageTag(appLanguage))
        calendar[Calendar.YEAR] = year
        calendar[Calendar.MONTH] = monthOfYear
        calendar[Calendar.DAY_OF_MONTH] = dayOfMonth

        return calendar.timeInMillis
    }

    return -1L
}

fun String.parseTime(): Long {
    val dates: List<String>? = removeLastCharacter(this)?.split("T")

    if (dates != null && dates.size > 1) {
        val calendar = Calendar.getInstance()
        calendar[Calendar.HOUR_OF_DAY] = dates[1].split(":").toTypedArray()[0].toInt()
        calendar[Calendar.MINUTE] = dates[1].split(":").toTypedArray()[1].toInt()
        calendar[Calendar.AM_PM] = calendar[Calendar.AM_PM]

        return calendar.timeInMillis
    }

    return -1L
}

fun formatDateString(date: Long, time: Long): String {
    val dateFormatter: DateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val timeFormatter: DateFormat = SimpleDateFormat("HH:mm", Locale.US)

    val dateString = dateFormatter.format(Date(date))
    val timeString = timeFormatter.format(Date(time))

    return dateString + "T" + timeString + ":00Z"
}

fun String?.getDays(planDayName: String?): List<OpenHour> {
    val daysResult = ArrayList<OpenHour>()
    val daysBeforeToday = ArrayList<OpenHour>()
    val daysAfterToday = ArrayList<OpenHour>()
    val daysToday = ArrayList<OpenHour>()
    val days = getWeekDaysAsDictionary()

    try {
        val parts = this!!.split("|").toTypedArray()

        for (part in parts) {
            val keyValues = part.split(": ").toTypedArray()
            val dayKeys = keyValues[0].split(",").toTypedArray()
            for (dayKey in dayKeys) {
                val `val` = arrayOf(keyValues[1].replace("[\n\r]".toRegex(), ""))
                days[dayKey.replace("[\n\r]".toRegex(), "").replace("\\s+".toRegex(), "")] = `val`
            }
        }

        val todayDayVal = if (!TextUtils.isEmpty(planDayName)) {
            val dayOfWeek = DayOfWeek.entries.find { TextUtils.equals(it.dayName, planDayName) }
            dayOfWeek?.dayNumber ?: 1
        } else {
            1
        }

        for ((key, value) in days.entries) {
            if (value.isEmpty()) {
                days[key.replace("\\s+".toRegex(), "")] = arrayOf("Closed")
            }
        }

        for (day in DayOfWeek.values()) {
            val keyVal: Int = day.dayNumber
            when {
                keyVal == todayDayVal -> {
                    daysToday.add(OpenHour().apply {
                        hour = getDayInNewLine(day.dayName, days[day.dayName]!![0])
                        isToday = true
                    })
                }
                keyVal > todayDayVal -> {
                    daysAfterToday.add(OpenHour().apply {
                        hour = getDayInNewLine(day.dayName, days[day.dayName]!![0])
                        isToday = false
                    })
                }
                else -> {
                    daysBeforeToday.add(OpenHour().apply {
                        hour = getDayInNewLine(day.dayName, days[day.dayName]!![0])
                        isToday = false
                    })
                }
            }
        }
    } catch (e: Exception) {
        return ArrayList()
    }

    daysResult.addAll(daysToday)
    daysResult.addAll(daysAfterToday)
    daysResult.addAll(daysBeforeToday)

    return daysResult
}

fun getWeekDaysAsDictionary(): MutableMap<String, Array<String>> {
    val dictionary: MutableMap<String, Array<String>> = HashMap()
    for (day in DayOfWeek.values()) {
        dictionary[day.dayName] = arrayOf()
    }

    return dictionary
}

//returns week days as a dictionary.
fun getDayInNewLine(dayName: String, dayValue: String): String {
    val dayInNewLine = java.lang.StringBuilder()
    dayInNewLine.append(dayName)
    dayInNewLine.append(" ")
    dayInNewLine.append(dayValue)
    return dayInNewLine.toString()
}

fun dayOfWeekContains(name: String): Boolean {
    for (c in DayOfWeek.entries) {
        if (TextUtils.equals(c.name, name)) {
            return true
        }
    }
    return false
}

var mondayText: String = "Mon"
var tuesdayText: String = "Tue"
var wednesdayText: String = "Wed"
var thursdayText: String = "Thu"
var fridayText: String = "Fri"
var saturdayText: String = "Sat"
var sundayText: String = "Sun"
var closedText: String = "Closed"
var appLanguage: String = "en"

enum class DayOfWeek(//Getters and Setters
    //Instance Variables
    //Associate a number with the day of the week.
    open val dayNumber: Int, val dayName: String
) {
    //Days of week and values associated with them.
    SUN(1, sundayText) {
        override operator fun next(): DayOfWeek {
            return MON
        }
    },
    MON(2, mondayText) {
        override operator fun next(): DayOfWeek {
            return TUE
        }
    },
    TUE(3, tuesdayText) {
        override operator fun next(): DayOfWeek {
            return WED
        }
    },
    WED(4, wednesdayText) {
        override operator fun next(): DayOfWeek {
            return THU
        }
    },
    THU(5, thursdayText) {
        override operator fun next(): DayOfWeek {
            return FRI
        }
    },
    FRI(6, fridayText) {
        override operator fun next(): DayOfWeek {
            return SAT
        }
    },
    SAT(7, saturdayText) {
        override operator fun next(): DayOfWeek {
            return SUN
        }
    };

    //new toString implementation
    override fun toString(): String {
        return dayName
    }

    //to implement in the enum values.
    open fun next(): DayOfWeek {
        return values()[(ordinal + 1) % values().size]
    }

}

//fun String.getBirthDateFromAge(): String {
//    val year = Calendar.getInstance().get(Calendar.YEAR)
//    val birthYear = year - (this.toIntOrNull() ?: 0)
//    return "$birthYear-01-01"
//}
//
//fun String.getAgeFromBirthDate(): String {
//    val dateFormatter: DateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
//    val cal = Calendar.getInstance()
//    cal.time = dateFormatter.parse(this) ?: Date()
//    val birthYear = cal.get(Calendar.YEAR)
//    val age = Calendar.getInstance().get(Calendar.YEAR) - birthYear
//    return age.toString()
//}

fun String.getDayMonthYear(): Triple<Int, Int, Int> {

    val dateArr: Array<String> = split("-").toTypedArray()
    val year = Integer.valueOf(dateArr[0])
    //month − Value to be used for MONTH field. 0 is January
    //month − Value to be used for MONTH field. 0 is January
    val monthOfYear = Integer.valueOf(dateArr[1]) - 1
    val dayOfMonth = Integer.valueOf(dateArr[2])
    return Triple(dayOfMonth, monthOfYear, year)
}

fun getDiffDay(cal2: Calendar, cal1: Calendar): Int {
//    val diff = cal2.time.time - cal1.time.time

    return cal2[Calendar.DAY_OF_YEAR] - cal1[Calendar.DAY_OF_YEAR]
//    return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)
}


fun getDiffDay(date2: Long, date1: Long): Long {
    val calendar1 = Calendar.getInstance()
    calendar1.timeInMillis = date1
    calendar1[Calendar.HOUR_OF_DAY] = 0
    calendar1[Calendar.MINUTE] = 0
    calendar1[Calendar.SECOND] = 0
    calendar1[Calendar.MILLISECOND] = 0
    val calendar2 = Calendar.getInstance()
    calendar2.timeInMillis = date2
    calendar2[Calendar.HOUR_OF_DAY] = 0
    calendar2[Calendar.MINUTE] = 0
    calendar2[Calendar.SECOND] = 0
    calendar2[Calendar.MILLISECOND] = 0
    val diff = calendar2.time.time - calendar1.time.time

    return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)
}

fun getHoursForTime(interval: Int = 30): java.util.ArrayList<String> {
    var hours = arrayListOf<String>()
    for (hour in 0 until 24) {
        for (min in 0 until 60 step interval) {
            val hour = getTimeForString(hour)
            val min = getTimeForString(min)
            hours.add("$hour:$min")
        }
    }

    return hours
}

private fun getTimeForString(time: Int): String {
    if (time < 10) {
        return "0$time"
    }
    return "$time"
}

fun getDateFromComponents(year: Int, monthOfYear: Int, dayOfMonth: Int): String {
    val month = if ((monthOfYear + 1) < 10) {
        "0${monthOfYear + 1}"
    } else {
        "${monthOfYear + 1}"
    }

    val day = if ((dayOfMonth) < 10) {
        "0$dayOfMonth"
    } else {
        "$dayOfMonth"
    }
    return "$year-$month-$day"
}