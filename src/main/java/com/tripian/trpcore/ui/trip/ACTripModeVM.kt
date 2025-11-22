package com.tripian.trpcore.ui.trip

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.text.TextUtils
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.maps.CoordinateBounds
import com.tripian.gyg.ui.ACExperiences
import com.tripian.one.api.trip.model.Trip
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.ExportPlan
import com.tripian.trpcore.domain.FetchTrip
import com.tripian.trpcore.domain.GetDirectionRoutes
import com.tripian.trpcore.domain.GetMapOffer
import com.tripian.trpcore.domain.GetMapStep
import com.tripian.trpcore.domain.GetMapStepAlternatives
import com.tripian.trpcore.domain.GetMyOffers
import com.tripian.trpcore.domain.GetMyOffersListener
import com.tripian.trpcore.domain.GetPlanDay
import com.tripian.trpcore.domain.GetTripPlans
import com.tripian.trpcore.domain.GetTripWarning
import com.tripian.trpcore.domain.LocationManager
import com.tripian.trpcore.domain.SearchThisArea
import com.tripian.trpcore.domain.model.MapStep
import com.tripian.trpcore.ui.trip.booking.ACBooking
import com.tripian.trpcore.ui.trip.favorite.ACFavorite
import com.tripian.trpcore.ui.trip.my_offers.ACMyOffers
import com.tripian.trpcore.ui.trip.places.ACPlaces
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.event.EventConstants
import com.tripian.trpcore.util.event.EventMessage
import com.tripian.trpcore.util.extensions.getDate
import com.tripian.trpcore.util.extensions.goBack
import com.tripian.trpcore.util.extensions.hideLoading
import com.tripian.trpcore.util.extensions.isHmsOnly
import com.tripian.trpcore.util.extensions.navigateToFragment
import com.tripian.trpcore.util.extensions.parseDate
import com.tripian.trpcore.util.extensions.showLoading
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import javax.inject.Inject

class ACTripModeVM @Inject constructor(
    val fetchTrip: FetchTrip,
    val app: Application,
    val getMapStep: GetMapStep,
    val getDirectionRoutes: GetDirectionRoutes,
    val getMapStepAlternatives: GetMapStepAlternatives,
    val getPlanDay: GetPlanDay,
    val locationManager: LocationManager,
    val searchThisArea: SearchThisArea,
    val getTripWarning: GetTripWarning,
    val getTripPlans: GetTripPlans,
    val getMapOffer: GetMapOffer,
    val getMyOffers: GetMyOffers,
    val getMyOffersListener: GetMyOffersListener,
    val exportPlan: ExportPlan
) : BaseViewModel(
    fetchTrip,
    getMapStep,
    getDirectionRoutes,
    getMapStepAlternatives,
    getPlanDay,
    locationManager,
    searchThisArea,
    getTripWarning,
    getMapOffer,
    getMyOffers,
    getMyOffersListener,
    exportPlan
) {

    var onSetMapPoiListener = MutableLiveData<List<MapStep>>()
    var onShowAlternativesListener = MutableLiveData<List<MapStep>>()
    var onHideAlternativesListener = MutableLiveData<Unit>()
    var onAnimateMapCameraListener = MutableLiveData<Unit>()
    var onShowRouteListener = MutableLiveData<DirectionsRoute>()
    var onRedirectRouteListener = MutableLiveData<Pair<DirectionsRoute, MapStep>>()
    var onAnimateAlternativeTextStateListener = MutableLiveData<Unit>()
    var onGoLocationListener = MutableLiveData<Location>()
    var onGoCityListener = MutableLiveData<Unit>()
    var onEnableLocationListener = MutableLiveData<Unit>()
    var onDisableLocationListener = MutableLiveData<Unit>()
    var onSetCityNameListener = MutableLiveData<String>()
    var onSetDateListener = MutableLiveData<String>()
    var onSearchThisAreaListener = MutableLiveData<Boolean>()
    var onShowSearchListener = MutableLiveData<List<MapStep>>()
    var onHideSearchListener = MutableLiveData<Unit>()
    var onClearSearchListener = MutableLiveData<Unit>()
    var onHideMapStepsListener = MutableLiveData<List<MapStep>>()

    var onFocusCityListener = MutableLiveData<Location>()
    var onShowOffersListener = MutableLiveData<List<MapStep>>()
    var onHideOffersListener = MutableLiveData<Unit>()
    var onExportPlanListener = MutableLiveData<String>()

    lateinit var trip: Trip

    private var isAlternativeShowing = false
    private var isAlternativeAnimated = false

    private var isOffersShowing = false
    private var mapOffers: List<MapStep>? = null

    private var steps: List<MapStep>? = null
    private var alternatives: List<MapStep>? = null
    private var searched: List<MapStep>? = null

    private var currentLocation: Location? = null

    private var searchBounds: CoordinateBounds? = null
    private var searchDistance: Double? = null

    private var currentPlanId: Int? = null

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        trip = arguments!!.getSerializable("trip") as Trip

        onSetCityNameListener.postValue(trip.city?.name)

        getMapStep.on(success = { steps ->
            this.steps = steps

            onSetMapPoiListener.postValue(steps)

            getDirectionRoutes.on(GetDirectionRoutes.Params(steps = steps), success = {
                onShowRouteListener.postValue(it)
            }, error = {
                if (it.type == AlertType.DIALOG) {
                    showDialog(contentText = it.errorDesc)
                } else {
                    showAlert(AlertType.ERROR, it.errorDesc)
                }
            })

            // Show alternative button animation
            if (!isAlternativeAnimated) {
                isAlternativeAnimated = true
                // todo. alternatives removed
                onAnimateAlternativeTextStateListener.postValue(Unit)
            }

            // Map camera zoom to markers
            onAnimateMapCameraListener.postValue(Unit)

            // if alternatives markes is showing, hide them
            if (isAlternativeShowing) {
                isAlternativeShowing = false

                alternatives?.let { onHideMapStepsListener.postValue(alternatives!!) }
                onHideAlternativesListener.postValue(Unit)
            }
        })

        getPlanDay.on(success = {
            currentPlanId = it.first
            val dayTitle = it.second
            onSetDateListener.postValue(dayTitle)
        })

        getTripWarning.on(success = {
            if (!TextUtils.isEmpty(it)) {
                showAlert(AlertType.INFO, it)
            }
        })
    }

    fun onMapLoaded() {

        onFocusCityListener.postValue(Location("").apply {
            latitude = trip.city?.coordinate?.lat ?: 0.0
            longitude = trip.city?.coordinate?.lng ?: 0.0
        })
        showLoading()

        fetchTrip.on(FetchTrip.Params(trip.tripHash!!), success = {
            triggerLocation()

            hideLoading()
        }, error = {
            hideLoading()

            if (it.type == AlertType.DIALOG) {
                showDialog(contentText = it.errorDesc)
            } else {
                showAlert(AlertType.ERROR, it.errorDesc)
            }
        })

        val dateFrom = getDate(trip.tripProfile?.arrivalDatetime?.parseDate() ?: 0)
        val dateTo = getDate(trip.tripProfile?.departureDatetime?.parseDate() ?: 0)
        getMyOffers.on(
            GetMyOffers.Params(
                dateFrom = dateFrom,
                dateTo = dateTo,
                isClear = true
            )
        )
    }

    fun onClickedLocation() {
        if (currentLocation == null) {
            triggerLocation(true)
        } else {
            currentLocation?.let { onGoLocationListener.postValue(it) }
        }
    }

    private fun triggerLocation(isGoLocation: Boolean = false) {
        if (!isHmsOnly(app.applicationContext)) {
            locationManager.on(Unit, success = {
                // Location found
                currentLocation = it
                onEnableLocationListener.postValue(Unit)

                if (isGoLocation) {
                    currentLocation?.let { onGoLocationListener.postValue(it) }
                }
            }, error = {
                onDisableLocationListener.postValue(Unit)
            })
        }
    }

    fun onClickedBack() {
        goBack()
//        onEnableLocationListener.postValue(Unit)
    }

    fun onClickedAlternative() {
        if (isAlternativeShowing) {
            isAlternativeShowing = false
            onHideMapStepsListener.postValue(alternatives ?: arrayListOf())
            onHideAlternativesListener.postValue(Unit)
        } else {
            isAlternativeShowing = true
            showLoading()

            getMapStepAlternatives.on(success = {
                alternatives = it
                onShowAlternativesListener.postValue(it)

                hideLoading()
            }, error = {
                hideLoading()

                if (it.type == AlertType.DIALOG) {
                    showDialog(contentText = it.errorDesc)
                } else {
                    showAlert(AlertType.ERROR, it.errorDesc)
                }
            })
        }
    }

    fun onClickedExportPlan() {
        trip.tripHash?.let { tripHash ->
            currentPlanId?.let { planId ->
                showLoading()
                exportPlan.on(ExportPlan.Params(planId, tripHash),
                    success = {
                        hideLoading()
                        onExportPlanListener.postValue(it.data?.url)
                    },
                    error = {
                        hideLoading()

                        if (it.type == AlertType.DIALOG) {
                            showDialog(contentText = it.errorDesc)
                        } else {
                            showAlert(AlertType.ERROR, it.errorDesc)
                        }
                    })
            }
        }
    }

    fun onClickedDay() {
        navigateToFragment(FRDaySelect.newInstance())
    }

    fun onClickedChangeTime() {
        navigateToFragment(FRChangeTime.newInstance())
    }

    fun onMapItemClicked(mapStep: MapStep) {
        if (!mapStep.homeBase) {
            navigateToFragment(FRPoiView.newInstance(mapStep))
        }
    }

    fun onClickedOffers(bounds: CoordinateBounds) {
        if (isOffersShowing) {
            isOffersShowing = false
            onHideMapStepsListener.postValue(mapOffers ?: arrayListOf())
            onHideOffersListener.postValue(Unit)
        } else {
            showLoading()
            val dateFrom = getDate(trip.tripProfile?.arrivalDatetime?.parseDate() ?: 0)
            val dateTo = getDate(trip.tripProfile?.departureDatetime?.parseDate() ?: 0)
            getMapOffer.on(
                GetMapOffer.Params(
                    bounds = bounds,
                    dateFrom = dateFrom,
                    dateTo = dateTo
                ),
                success = {
                    if (it.isEmpty()) {
                        showAlert(
                            AlertType.INFO,
                            getLanguageForKey(LanguageConst.NO_OFFERS_FOUND)
                        )
                        hideLoading()
                        return@on
                    }
                    isOffersShowing = true
                    mapOffers = it
                    onShowOffersListener.postValue(it)

                    hideLoading()
                }, error = {
                    hideLoading()

                    if (it.type == AlertType.DIALOG) {
                        showDialog(contentText = it.errorDesc)
                    } else {
                        showAlert(AlertType.ERROR, it.errorDesc)
                    }
                })
        }
    }

    /**
     * LocationManager'da gerekli durumlar
     * - Permission,
     * - Open Location,
     * - LifecycleOwner
     */
    fun setLifecycleOwner(activity: Activity) {
        locationManager.setLifecycleOwner(activity)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        locationManager.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        locationManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    fun onClickedBackMap() {
        onGoCityListener.postValue(Unit)
    }

    fun onClickedItinerary() {
        navigateToFragment(FRItinerary.newInstance(if (steps.isNullOrEmpty()) ArrayList() else steps!!))
    }

    fun onClickedPlaces() {
        startActivity(ACPlaces::class, bundleOf(Pair("tripHash", trip.tripHash)))
    }

    fun onClickedFavorite() {
        startActivity(ACFavorite::class, bundleOf(Pair("cityId", trip.city?.id)))
    }

    fun onClickedBooking() {
        startActivity(ACBooking::class, bundleOf(Pair("cityId", trip.city?.id)))
    }

    fun onClickedMyOffers() {
        val dateFrom = getDate(trip.tripProfile?.arrivalDatetime?.parseDate() ?: 0)
        val dateTo = getDate(trip.tripProfile?.departureDatetime?.parseDate() ?: 0)
        startActivity(
            ACMyOffers::class,
            bundleOf(Pair("dateFrom", dateFrom), Pair("dateTo", dateTo))
        )
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public fun onLocationRouteEvent(item: EventMessage<MapStep>) {
        if (item.tag == EventConstants.LocationRedirect && currentLocation != null) {
            getDirectionRoutes.on(
                GetDirectionRoutes.Params(
                    origin = currentLocation,
                    destination = item.value!!
                ), success = {
                    onRedirectRouteListener.postValue(Pair(it, item.value!!))

                    currentLocation?.let { onGoLocationListener.postValue(it) }
                })
        }
    }

    fun onZoomLevelChanged(newZoom: Double) {
        onSearchThisAreaListener.postValue(newZoom > 12)
    }

    fun onClickedSearchThisArea(bounds: CoordinateBounds, distance: Double) {
        searchBounds = bounds
        searchDistance = distance

        navigateToFragment(FRSearchCategory.newInstance())
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public fun onSearchCategoryEvent(item: EventMessage<Int>) {
        if (item.tag == EventConstants.SearchCategory) {
            showLoading()

            searchThisArea.on(
                SearchThisArea.Params(
                    searchBounds!!, searchDistance!!, if (item.value == 999) {
                        null
                    } else {
                        arrayListOf(item.value!!)
                    }
                ), success = {
                    if (!searched.isNullOrEmpty()) {
                        searched?.let { onHideMapStepsListener.postValue(it) }
                    }

                    searched = it

                    if (searched.isNullOrEmpty()) {
                        showAlert(
                            AlertType.INFO,
                            getLanguageForKey(LanguageConst.NO_RESULT_IN_AREA)
                        )
                    }

                    onHideSearchListener.postValue(Unit)

                    onShowSearchListener.postValue(it)

                    onHideSearchListener.postValue(Unit)

                    hideLoading()
                }, error = {
                    hideLoading()
                }
            )
        }
    }

    fun onClickedClearSearch() {
        searched?.let { onHideMapStepsListener.postValue(it) }
        onClearSearchListener.postValue(Unit)
    }

    fun onClickedExperiences() {
        if (::trip.isInitialized) {
            getTripPlans.on(success = {
                val currentPlan = it.second[it.first]
                if (!trip.city?.name.isNullOrEmpty() && currentPlan.date != null) {
                    startActivity(
                        ACExperiences::class,
                        bundleOf(Pair("city", trip.city?.name!!), Pair("date", currentPlan.date))
                    )
                }
            })
        }
    }

    fun isGoogleMapExisted(): Boolean {
        val pm: PackageManager = app.applicationContext.packageManager
        try {
            pm.getPackageInfo("com.google.android.apps.maps", PackageManager.GET_META_DATA)
        } catch (e: PackageManager.NameNotFoundException) {
            return false
        }
        return true
    }
}
