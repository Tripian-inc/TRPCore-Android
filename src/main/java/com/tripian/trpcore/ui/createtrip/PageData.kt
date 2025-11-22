package com.tripian.trpcore.ui.createtrip

import com.tripian.one.api.cities.model.City
import com.tripian.one.api.companion.model.Companion
import com.tripian.one.api.trip.model.Accommodation
import com.tripian.one.api.trip.model.Trip
import com.tripian.trpcore.domain.model.BaseModel
import com.tripian.trpcore.util.extensions.formatTime
import com.tripian.trpcore.util.extensions.getDay
import com.tripian.trpcore.util.extensions.getDiffDay
import com.tripian.trpcore.util.extensions.getMonthName
import com.tripian.trpcore.util.extensions.getYear
import java.util.Calendar

/**
 * Created by semihozkoroglu on 21.08.2020.
 */
class PageData : BaseModel() {
    var trip: Trip? = null

    var city: City? = null
    var place: Accommodation? = null

    var adult: Int = 1
    var child: Int = 0

    var arrivalDate = -1L
    var departureDate = -1L
    var arrivalTime = -1L
    var departureTime = -1L

    var pace: String? = null
    var companions: List<Companion>? = null
    var answers: List<Int>? = null

    fun getCompanionNames(): String {
        var count = 0
        var names = ""

        if (companions != null) {
            for (c in companions!!.iterator()) {
                count++
                names += if (count == companions!!.size) {
                    c.name
                } else {
                    c.name + ","
                }
            }
        }

        return names
    }

    fun getCompanionNameList(): List<String> {
        if (companions != null) {
            return companions!!.map { it.name ?: "" }
        }

        return listOf()
    }

    fun getArrivalDate(): Triple<String, String, String> {
        if (arrivalDate == -1L) {
            val cal = Calendar.getInstance()
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            if (hour >= 16) {
                cal.add(Calendar.DAY_OF_MONTH, 1)
            }

            arrivalDate = cal.timeInMillis
        }

        return Triple(arrivalDate.getDay(), arrivalDate.getMonthName(), arrivalDate.getYear())
    }

    fun getArrivalTime(): String {
        if (arrivalTime == -1L) {
            val cal = Calendar.getInstance()
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)
            cal[Calendar.HOUR_OF_DAY] = 9
            cal[Calendar.MINUTE] = 0
            if (getDiffDay(cal.timeInMillis, arrivalDate) == 0L) {
                if (hour in 10..15) {
                    if (minute < 30) {
                        cal[Calendar.HOUR_OF_DAY] = hour
                        cal[Calendar.MINUTE] = 30
                    } else {
                        cal[Calendar.HOUR_OF_DAY] = hour + 1
                        cal[Calendar.MINUTE] = 0
                    }
                }
            }
            cal[Calendar.AM_PM] = cal[Calendar.AM_PM]
            arrivalTime = cal.timeInMillis
        }

        return arrivalTime.formatTime()
    }

    fun getDepartureDate(): Triple<String, String, String> {
        if (departureDate == -1L) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_MONTH, 1)

            departureDate = cal.timeInMillis
        }

        return Triple(departureDate.getDay(), departureDate.getMonthName(), departureDate.getYear())
    }

    fun getDepartureTime(): String {
        if (departureTime == -1L) {
            val cal = Calendar.getInstance()
            cal[Calendar.HOUR_OF_DAY] = 21
            cal[Calendar.MINUTE] = 0
            cal[Calendar.AM_PM] = cal[Calendar.AM_PM]
            departureTime = cal.timeInMillis
        }

        return departureTime.formatTime()
    }
}