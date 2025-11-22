package com.tripian.trpcore.ui.createtrip

import android.os.Bundle
import android.text.TextUtils
import androidx.lifecycle.MutableLiveData
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.GetCities
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.event.EventConstants
import com.tripian.trpcore.util.event.EventMessage
import com.tripian.trpcore.util.extensions.getDiffDate
import com.tripian.trpcore.util.extensions.getDiffDay
import com.tripian.trpcore.util.extensions.hideLoading
import com.tripian.trpcore.util.extensions.hourToMillis
import com.tripian.trpcore.util.extensions.navigateToFragment
import com.tripian.trpcore.util.extensions.showLoading
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.Calendar
import javax.inject.Inject

class FRCreateTripDestinationVM @Inject constructor(
    private val getCities: GetCities
) : BaseViewModel() {

    @Inject
    lateinit var pageData: PageData

    var onSetArrivalDateListener = MutableLiveData<Triple<String, String, String>>()
    var onSetDepartureDateListener = MutableLiveData<Triple<String, String, String>>()
    var onOpenArrivalDateListener = MutableLiveData<Pair<Long, Long>>()
    var onOpenDepartureDateListener = MutableLiveData<Pair<Pair<Long, Long>, Int>>()
    var onSetArrivalTimeListener = MutableLiveData<String>()
    var onSetDepartureTimeListener = MutableLiveData<String>()
    var onSetDestinationListener = MutableLiveData<String>()

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        setDateInfo()
        setTimeInfo()
        setCities()

    }

    private fun setDateInfo() {
        onSetArrivalDateListener.postValue(pageData.getArrivalDate())
        onSetDepartureDateListener.postValue(pageData.getDepartureDate())
    }

    private fun setTimeInfo() {
        onSetArrivalTimeListener.postValue(pageData.getArrivalTime())
        onSetDepartureTimeListener.postValue(pageData.getDepartureTime())
    }

    private fun setCities() {
        showLoading()
        getCities.on(success = {
            if (it.first) {
                hideLoading()
            }
        }, error = {
            hideLoading()

            if (it.type == AlertType.DIALOG) {
                showDialog(contentText = it.errorDesc)
            } else {
                showAlert(AlertType.ERROR, it.errorDesc)
            }
        })
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public fun onTimeSelected(item: EventMessage<Pair<String, String>>) {
        if (item.tag == EventConstants.TimePicker) {
            if (TextUtils.equals(item.value!!.first, "arrival")) {
                pageData.arrivalTime = item.value!!.second.hourToMillis()
            } else if (TextUtils.equals(item.value!!.first, "departure")) {
                pageData.departureTime = item.value!!.second.hourToMillis()
            }

            setTimeInfo()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public fun onDestinationSelected(item: EventMessage<Unit>) {
        if (item.tag == EventConstants.DestinationCitySelect) {
            onSetDestinationListener.postValue(pageData.city?.name)
        }
    }

    fun onClickedDestination() {
        navigateToFragment(FRCitySelect.newInstance())
    }

    fun onClickedArrivalTime(time: String) {
        navigateToFragment(
            FRTimePicker.newInstance(
                "arrival",
                getLanguageForKey(LanguageConst.SELECT_ARRIVAL_HOUR),
                time
            )
        )
    }

    fun onClickedDepartureTime(time: String) {
        navigateToFragment(
            FRTimePicker.newInstance(
                "departure",
                getLanguageForKey(LanguageConst.SELECT_DEPARTURE_HOUR),
                time
            )
        )
    }

    fun onClickedArrivalDate() {
        val today = Calendar.getInstance()

        onOpenArrivalDateListener.postValue(Pair(pageData.arrivalDate, today.timeInMillis))
    }

    fun onClickedDepartureDate() {
        onOpenDepartureDateListener.postValue(
            Pair(
                Pair(
                    pageData.departureDate,
                    pageData.arrivalDate
                ), (pageData.city?.maxTripDays ?: 3) - 1
            )
        )
    }

    fun onArrivalDateSelected(dayOfMonth: Int, monthOfYear: Int, year: Int) {
        var calendar = Calendar.getInstance()

        calendar[Calendar.YEAR] = year
        calendar[Calendar.MONTH] = monthOfYear
        calendar[Calendar.DAY_OF_MONTH] = dayOfMonth
        pageData.arrivalDate = calendar.timeInMillis

        if (getDiffDay(calendar, Calendar.getInstance()) < 0) {
            calendar = Calendar.getInstance()
        }

        val diff = getDiffDate(pageData.departureDate, pageData.arrivalDate)
        val maxTripDays = pageData.city?.maxTripDays ?: 3
        if (diff == -1L) {
            val calendarDeparture = Calendar.getInstance()
            calendarDeparture.timeInMillis = pageData.arrivalDate
            pageData.departureDate = calendarDeparture.timeInMillis
        } else if (diff > maxTripDays) {
            val calendarDeparture = Calendar.getInstance()
            calendarDeparture.timeInMillis = pageData.arrivalDate
            calendarDeparture.add(Calendar.DAY_OF_MONTH, maxTripDays - 1)
            pageData.departureDate = calendarDeparture.timeInMillis
        }

        setDateInfo()
    }

    fun onDepartureDateSelected(dayOfMonth: Int, monthOfYear: Int, year: Int) {
        val calendar = Calendar.getInstance()

        calendar[Calendar.YEAR] = year
        calendar[Calendar.MONTH] = monthOfYear
        calendar[Calendar.DAY_OF_MONTH] = dayOfMonth
        pageData.departureDate = calendar.timeInMillis

        setDateInfo()
    }
}
