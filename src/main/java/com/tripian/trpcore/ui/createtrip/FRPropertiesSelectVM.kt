package com.tripian.trpcore.ui.createtrip

import android.os.Bundle
import android.text.TextUtils
import androidx.lifecycle.MutableLiveData
import com.google.android.libraries.places.api.model.Place
import com.tripian.one.api.cities.model.City
import com.tripian.one.api.companion.model.Companion
import com.tripian.one.api.pois.model.Coordinate
import com.tripian.one.api.trip.model.Accommodation
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.UserCompanionListener
import com.tripian.trpcore.ui.companion.ACManageCompanion
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.event.EventConstants
import com.tripian.trpcore.util.event.EventMessage
import com.tripian.trpcore.util.extensions.getDiffDate
import com.tripian.trpcore.util.extensions.hourToMillis
import com.tripian.trpcore.util.extensions.hours
import com.tripian.trpcore.util.extensions.navigateToFragment
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.Serializable
import java.util.Calendar
import javax.inject.Inject

class FRPropertiesSelectVM @Inject constructor(val userCompanionListener: UserCompanionListener) :
    BaseViewModel(userCompanionListener) {

    @Inject
    lateinit var pageData: PageData

    var onSetArrivalDateListener = MutableLiveData<Triple<String, String, String>>()
    var onSetDepartureDateListener = MutableLiveData<Triple<String, String, String>>()
    var onOpenArrivalDateListener = MutableLiveData<Pair<Long, Long>>()
    var onOpenDepartureDateListener = MutableLiveData<Pair<Long, Long>>()
    var onSetArrivalTimeListener = MutableLiveData<String>()
    var onSetDepartureTimeListener = MutableLiveData<String>()
    var onSetCompanionCountListener = MutableLiveData<String>()
    var onShowSearchListener = MutableLiveData<City>()
    var onSetPlaceListener = MutableLiveData<String>()
    var onSetCompanionListener = MutableLiveData<String>()

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        setDateInfo()
        setTimeInfo()
        setPlaceInfo()
        setCompanionsInfo()
        setNumberCompanion()

        userCompanionListener.on(success = { c ->
            val companions = ArrayList<Companion>()

            pageData.companions?.forEach { p ->
                if (c.data != null && c.data!!.any { p.id == it.id }) {
                    companions.add(p)
                }
            }

            pageData.companions = companions
            setCompanionsInfo()
        })
    }

    private fun setNumberCompanion() {
//        val child = if (pageData.child > 0) {
//            if (pageData.child == 1) {
//                "${pageData.child} " + strings.getString(R.string.child)
//            } else {
//                "${pageData.child} " + strings.getString(R.string.childs)
//            }
//        } else {
//            strings.getString(R.string.no_child)
//        }
//
//        val adult = if (pageData.adult == 1) {
//            "${pageData.adult} " + strings.getString(R.string.adult)
//        } else {
//            "${pageData.adult} " + strings.getString(R.string.adults)
//        }

        onSetCompanionCountListener.postValue(
            "${getLanguageForKey(LanguageConst.ADULTS)}, ${
                getLanguageForKey(
                    LanguageConst.CHILDREN
                )
            }"
        )
    }

    private fun setDateInfo() {
        onSetArrivalDateListener.postValue(pageData.getArrivalDate())
        onSetDepartureDateListener.postValue(pageData.getDepartureDate())
    }

    private fun setTimeInfo() {
        onSetArrivalTimeListener.postValue(pageData.getArrivalTime())
        onSetDepartureTimeListener.postValue(pageData.getDepartureTime())
    }

    private fun setPlaceInfo() {
        onSetPlaceListener.postValue(pageData.place?.name)
    }

    private fun setCompanionsInfo() {
        onSetCompanionListener.postValue(pageData.getCompanionNames())
    }

    fun onClickedSearch() {
        onShowSearchListener.postValue(pageData.city)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public fun onPlaceReceived(item: EventMessage<Place>) {
        if (item.tag == EventConstants.GooglePlace) {
            val place = item.value

            if (place != null) {
                pageData.place = Accommodation()
                pageData.place!!.refID = place.id
                pageData.place!!.name = place.displayName
                pageData.place!!.address = place.formattedAddress
                pageData.place!!.coordinate = Coordinate().apply {
                    place.location?.let {
                        lat = it.latitude
                        lng = it.longitude
                    } ?: run {
                        lat = 0.0
                        lng = 0.0
                    }
                }

                setPlaceInfo()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public fun onCompanionSelected(item: EventMessage<List<Companion>>) {
        if (item.tag == EventConstants.CompanionPicker) {
            val companions = item.value

            if (companions != null) {
                pageData.companions = companions

                setCompanionsInfo()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public fun onTimeSelected(item: EventMessage<Pair<String, Int>>) {
        if (item.tag == EventConstants.TimePicker) {
            if (TextUtils.equals(item.value!!.first, "arrival")) {
                pageData.arrivalTime = hours[item.value!!.second].hourToMillis()
            } else if (TextUtils.equals(item.value!!.first, "departure")) {
                pageData.departureTime = hours[item.value!!.second].hourToMillis()
            }

            setTimeInfo()
        }
    }

    fun onClickedCompanionSelect() {
//        navigateToFragment(FRCompanionSelect.newInstance(pageData.companions), animation = AnimationType.ENTER_FROM_RIGHT)
        startActivity(ACManageCompanion::class, Bundle().apply {
            pageData.companions?.let {
                putSerializable("companions", it as Serializable)
            }
        })
    }

    fun onClickedArrivalTime() {
//        navigateToFragment(FRTimePicker.newInstance("arrival"))
        navigateToFragment(
            FRTimePicker.newInstance(
                tag = "arrival",
                buttonText = getLanguageForKey(LanguageConst.SELECT_ARRIVAL_HOUR),
                initTime = "09:00"
            )
        )
    }

    fun onClickedDepartureTime() {
//        navigateToFragment(FRTimePicker.newInstance("departure"))
        navigateToFragment(
            FRTimePicker.newInstance(
                tag = "departure",
                buttonText = getLanguageForKey(LanguageConst.SELECT_DEPARTURE_HOUR),
                initTime = "21:00"
            )
        )
    }

    fun onClickedArrivalDate() {
        val today = Calendar.getInstance()

        onOpenArrivalDateListener.postValue(Pair(pageData.arrivalDate, today.timeInMillis))
    }

    fun onClickedDepartureDate() {
        onOpenDepartureDateListener.postValue(Pair(pageData.departureDate, pageData.arrivalDate))
    }

    fun onArrivalDateSelected(dayOfMonth: Int, monthOfYear: Int, year: Int) {
        val calendar = Calendar.getInstance()

        calendar[Calendar.YEAR] = year
        calendar[Calendar.MONTH] = monthOfYear
        calendar[Calendar.DAY_OF_MONTH] = dayOfMonth
        pageData.arrivalDate = calendar.timeInMillis

        if (getDiffDate(pageData.departureDate, pageData.arrivalDate) == -1L || getDiffDate(
                pageData.departureDate,
                pageData.arrivalDate
            ) > 14
        ) {
            val calendarDeparture = Calendar.getInstance()
            calendarDeparture.timeInMillis = pageData.arrivalDate
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

    fun onTextChanged(text: String) {
        if (TextUtils.isEmpty(text)) {
            pageData.place = null
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public fun onCompanionCountSelected(item: EventMessage<Unit>) {
        if (item.tag == EventConstants.CompanionCountSelect) {
            setNumberCompanion()
        }
    }

    fun onClickedCompanionCount() {
//        navigateToFragment(FRCompanionCount.newInstance())
    }

    fun onClickedCancelHotel() {
        pageData.place = null

        setPlaceInfo()
    }
}
