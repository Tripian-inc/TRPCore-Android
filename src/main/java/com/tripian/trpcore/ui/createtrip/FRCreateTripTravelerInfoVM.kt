package com.tripian.trpcore.ui.createtrip

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import com.google.android.libraries.places.api.model.Place
import com.tripian.one.api.companion.model.Companion
import com.tripian.one.api.pois.model.Coordinate
import com.tripian.one.api.trip.model.Accommodation
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.UserCompanions
import com.tripian.trpcore.ui.companion.CompanionMode
import com.tripian.trpcore.ui.companion.FRCompanions
import com.tripian.trpcore.ui.companion.FRNewCompanion
import com.tripian.trpcore.util.event.EventConstants
import com.tripian.trpcore.util.event.EventMessage
import com.tripian.trpcore.util.extensions.navigateToFragment
import com.tripian.trpcore.util.extensions.remove
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import javax.inject.Inject

class FRCreateTripTravelerInfoVM @Inject constructor(val userCompanions: UserCompanions) : BaseViewModel(userCompanions) {

    @Inject
    lateinit var pageData: PageData

    var onSetAdultListener = MutableLiveData<String>()
    var onSetChildListener = MutableLiveData<String>()

    var onSetPlaceListener = MutableLiveData<String>()
    var onSetCompanionListener = MutableLiveData<List<String>>()

    private var adultCount = 0
    private var childCount = 0


    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        adultCount = pageData.adult
        childCount = pageData.child

        onSetAdultListener.postValue("$adultCount")
        onSetChildListener.postValue("$childCount")

        setPlaceInfo()
        setCompanionsInfo()


        userCompanions.on(success = { c ->
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

    private fun setPlaceInfo() {
        onSetPlaceListener.postValue(pageData.place?.name)
    }

    private fun setCompanionsInfo() {
        onSetCompanionListener.postValue(pageData.getCompanionNameList())
    }

    fun onClickedSearch() {
        navigateToFragment(fragment = FRSearchAddress.newInstance(city = pageData.city!!))
//        startActivity(ACSearchAddress::class, Bundle().apply {
//            pageData.city?.let {
//                putSerializable("city", it as Serializable)
//            }
//        })
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
    public fun onCompanionCreated(item: EventMessage<Companion>) {
        if (item.tag == EventConstants.CompanionCreate) {
            val companion = item.value

            if (companion != null) {
                if (pageData.companions == null) {
                    pageData.companions = listOf(companion)
                } else {
//                    val companions = pageData.companions!!.toMutableList()
//                    companions.add(companion)
                    pageData.companions = pageData.companions!! + companion
//                    pageData.companions = companions
                }

                setCompanionsInfo()
            }
        }
    }

    fun onClickedCompanionSelect() {

        navigateToFragment(
            fragment = FRCompanions.newInstance(
                pageData.companions,
                CompanionMode.TRIP,
                onCreateCompanion = {
                    onClickedCompanionCreate()
                }
            )
        )
    }

    fun oncClickedCompanionRemove(companionName: String) {
        val tmpCompanions = pageData.companions!!.toMutableList()
        tmpCompanions.remove { it.name == companionName }
        pageData.companions = tmpCompanions
        setCompanionsInfo()
    }

    fun onClickedCompanionCreate() {
        navigateToFragment(fragment = FRNewCompanion.newInstance())
    }


    fun onClickedMinusAdult() {
        if (adultCount > 1) {
            onSetAdultListener.postValue("${--adultCount}")
            pageData.adult = adultCount
        }
    }

    fun onClickedPlusAdult() {
        if (adultCount < 99) {
            onSetAdultListener.postValue("${++adultCount}")
            pageData.adult = adultCount
        }
    }

    fun onClickedMinusChild() {
        if (childCount > 0) {
            onSetChildListener.postValue("${--childCount}")
            pageData.child = childCount
        }
    }

    fun onClickedPlusChild() {
        if (childCount < 99) {
            onSetChildListener.postValue("${++childCount}")
            pageData.child = childCount
        }
    }
}
