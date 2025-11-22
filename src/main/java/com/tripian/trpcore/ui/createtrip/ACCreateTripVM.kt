package com.tripian.trpcore.ui.createtrip

import android.os.Bundle
import androidx.core.os.bundleOf
import com.tripian.one.api.companion.model.Companion
import com.tripian.one.api.trip.model.Trip
import com.tripian.one.api.trip.model.isGenerated
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.CreateTrip
import com.tripian.trpcore.domain.FetchButterflyTrip
import com.tripian.trpcore.domain.UpdateTrip
import com.tripian.trpcore.domain.UserCompanions
import com.tripian.trpcore.ui.trip.ACTripMode
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.CreateTripSteps
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.dialog.DGActionListener
import com.tripian.trpcore.util.event.EventConstants
import com.tripian.trpcore.util.event.EventMessage
import com.tripian.trpcore.util.extensions.getSerializableCompat
import com.tripian.trpcore.util.extensions.goBack
import com.tripian.trpcore.util.extensions.hideLoading
import com.tripian.trpcore.util.extensions.navigateToFragment
import com.tripian.trpcore.util.extensions.parseDate
import com.tripian.trpcore.util.extensions.parseTime
import com.tripian.trpcore.util.extensions.showLoading
import javax.inject.Inject

class ACCreateTripVM @Inject constructor(
    val userCompanions: UserCompanions,
    val createTrip: CreateTrip,
    val updateTrip: UpdateTrip,
    val fetchTrip: FetchButterflyTrip
) : BaseViewModel(userCompanions, createTrip, updateTrip, fetchTrip) {

    @Inject
    lateinit var pageData: PageData

    //    private var currentPage: Int = 0
    private var currentStep: CreateTripSteps = CreateTripSteps.DESTINATION
    private var frCreateTripItineraryProfile: FRCreateTripItineraryProfile? = null

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        if (arguments?.containsKey("trip") == true) {

            arguments?.getSerializableCompat<Trip>("companions")?.let { trip ->
                pageData.trip = trip
                pageData.city = trip.city
                pageData.place = trip.tripProfile?.accommodation
                pageData.pace = trip.tripProfile?.pace

                pageData.adult = trip.tripProfile?.numberOfAdults ?: 0
                pageData.child = trip.tripProfile?.numberOfChildren ?: 0

                if (trip.tripProfile?.arrivalDatetime.isNullOrEmpty().not()) {
                    pageData.arrivalDate =
                        trip.tripProfile?.arrivalDatetime?.parseDate() ?: 0L
                }
                if (trip.tripProfile?.departureDatetime.isNullOrEmpty().not()) {
                    pageData.departureDate =
                        trip.tripProfile?.departureDatetime?.parseDate() ?: 0L
                }
                if (trip.tripProfile?.arrivalDatetime.isNullOrEmpty().not()) {
                    pageData.arrivalTime =
                        trip.tripProfile?.arrivalDatetime?.parseTime() ?: 0L
                }
                if (trip.tripProfile?.departureDatetime.isNullOrEmpty().not()) {
                    pageData.departureTime =
                        trip.tripProfile?.departureDatetime?.parseTime() ?: 0L
                }

                pageData.answers = trip.tripProfile?.answers
                pageData.companions = ArrayList()

                if (trip.tripProfile?.companionIds != null && trip.tripProfile!!.companionIds!!.isNotEmpty()) {
                    userCompanions.on(success = {
                        val companions = ArrayList<Companion>()
                        it.data?.forEach { c ->
                            if (trip.tripProfile!!.companionIds!!.contains(c.id)) {
                                companions.add(c)
                            }
                        }

                        pageData.companions = companions

                    }, error = {
                        hideLoading()
                    })
                }
            }
        }
        navigateToFragment(fragment = FRCreateTripDestination.newInstance(), addToBackStack = false)
    }

    fun onClickedBack() {
        goBack()
    }

    fun onClickedNext() {
        if (currentStep == CreateTripSteps.DESTINATION) {
            if (pageData.city == null) {
                showAlert(AlertType.ERROR, getLanguageForKey(LanguageConst.SELECT_DESTINATION_CITY))
                return
            }
            navigateToFragment(fragment = FRCreateTripTravelerInfo.newInstance())
        } else if (currentStep == CreateTripSteps.TRAVELER_INFO) {
//            if (pageData.place == null) {
//                showAlert(
//                    AlertType.ERROR,
//                    getLanguageForKey(LanguageConst.SELECT_START_LOCATION)
//                )
//                return
//            }
            frCreateTripItineraryProfile = FRCreateTripItineraryProfile.newInstance()
            navigateToFragment(fragment = frCreateTripItineraryProfile!!)
        } else if (currentStep == CreateTripSteps.ITINERARY_PROFILE) {
            if (frCreateTripItineraryProfile!!.canClickNext()) {
                navigateToFragment(fragment = FRCreateTripPersonalInterests.newInstance())
            }
        } else {
            createOrUpdateTrip()
        }
    }

    private fun createOrUpdateTrip() {
        if (pageData.trip == null) {
            createTrip()
        } else {
            updateTrip()
        }
    }

    private fun createTrip() {
        showLoading()

        createTrip.on(
            CreateTrip.Params(
                pageData.city,
                pageData.place,
                pageData.adult,
                pageData.child,
                pageData.arrivalDate,
                pageData.departureDate,
                pageData.arrivalTime,
                pageData.departureTime,
                pageData.pace,
                pageData.companions,
                pageData.answers
            ), success = {

                eventBus.post(EventMessage(EventConstants.UpdateTrip, Unit))

                it.data?.tripHash?.let { tripHash ->
                    checkCreateTripStatus(tripHash)
                } ?: hideLoading()
            }, error = {
                hideLoading()

                if (it.type == AlertType.DIALOG) {
                    showDialog(contentText = it.errorDesc)
                } else {
                    showAlert(AlertType.ERROR, it.errorDesc)
                }
            })
    }

    private fun updateTrip() {
        val params = UpdateTrip.Params(
            pageData.trip,
            pageData.city,
            pageData.place,
            pageData.adult,
            pageData.child,
            pageData.arrivalDate,
            pageData.departureDate,
            pageData.arrivalTime,
            pageData.departureTime,
            pageData.pace,
            pageData.companions,
            pageData.answers
        )

        if (updateTrip.doNotGenerate(params) == 0) {
            showDialog(
                title = getLanguageForKey(LanguageConst.WARNING),
                contentText = getLanguageForKey(LanguageConst.TRIP_WILL_UPDATE),
                negativeBtn = getLanguageForKey(LanguageConst.CANCEL),
                positiveBtn = getLanguageForKey(LanguageConst.CONTINUE),
                positive = object : DGActionListener {
                    override fun onClicked(o: Any?) {
                        sendUpdateRequest(params)
                    }
                })
        } else {
            sendUpdateRequest(params)
        }
    }

    private fun sendUpdateRequest(params: UpdateTrip.Params) {
        showLoading()

        updateTrip.on(params, success = {
//            hideLoading()

            eventBus.post(EventMessage(EventConstants.UpdateTrip, Unit))

            it.data?.tripHash?.let { tripHash ->
                checkCreateTripStatus(tripHash)
            } ?: hideLoading()
//            startActivity(ACTripMode::class, bundleOf(Pair("trip", it.data)))
//            finishActivity()
        }, error = {
            hideLoading()

            if (it.type == AlertType.DIALOG) {
                showDialog(contentText = it.errorDesc)
            } else {
                showAlert(AlertType.ERROR, it.errorDesc)
            }
        })
    }

    private fun checkCreateTripStatus(tripHash: String) {
        fetchTrip.on(FetchButterflyTrip.Params(tripHash), success = {
            val trip = it.data

            trip?.plans?.firstOrNull()?.let { plan ->
                if (plan.isGenerated()) {
                    hideLoading()
                    startActivity(ACTripMode::class, bundleOf(Pair("trip", trip)))
                    finishActivity()
                }
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

    fun onSetCurrentPage(step: CreateTripSteps) {
        currentStep = step
    }
}
