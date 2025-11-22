package com.tripian.trpcore.ui.trip_detail

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import com.tripian.gyg.domain.model.ExperiencesItem
import com.tripian.gyg.ui.ACExperienceDetail
import com.tripian.one.api.favorites.model.Favorite
import com.tripian.one.api.offers.model.Offer
import com.tripian.one.api.pois.model.Booking
import com.tripian.one.api.pois.model.Image
import com.tripian.one.api.pois.model.Product
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.AddFavorite
import com.tripian.trpcore.domain.AddOffer
import com.tripian.trpcore.domain.AddStep
import com.tripian.trpcore.domain.DeleteFavorite
import com.tripian.trpcore.domain.DeleteStep
import com.tripian.trpcore.domain.DeleteUserReservation
import com.tripian.trpcore.domain.FavoriteListener
import com.tripian.trpcore.domain.GetBookingInfo
import com.tripian.trpcore.domain.GetMyOffers
import com.tripian.trpcore.domain.GetMyOffersListener
import com.tripian.trpcore.domain.GetPlaceDetail
import com.tripian.trpcore.domain.GetTripianUser
import com.tripian.trpcore.domain.GetUserReservation
import com.tripian.trpcore.domain.InCurrentCity
import com.tripian.trpcore.domain.LocationManager
import com.tripian.trpcore.domain.RemoveOffer
import com.tripian.trpcore.domain.SaveUserReservation
import com.tripian.trpcore.domain.StepListener
import com.tripian.trpcore.domain.UpdateStepOrder
import com.tripian.trpcore.domain.model.OpenHour
import com.tripian.trpcore.domain.model.PlaceBooking
import com.tripian.trpcore.domain.model.PlaceDetail
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.trpcore.ui.common.ACWebPage
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.event.EventConstants
import com.tripian.trpcore.util.event.EventMessage
import com.tripian.trpcore.util.extensions.goBack
import com.tripian.trpcore.util.extensions.hideLoading
import com.tripian.trpcore.util.extensions.isHmsOnly
import com.tripian.trpcore.util.extensions.roundTo
import com.tripian.trpcore.util.extensions.showLoading
import javax.inject.Inject

class ACTripDetailVM @Inject constructor(
    val getPlaceDetail: GetPlaceDetail,
    val addFavorite: AddFavorite,
    val deleteFavorite: DeleteFavorite,
    val favoriteListener: FavoriteListener,
    val addStep: AddStep,
    val deleteStep: DeleteStep,
    val updateStepOrder: UpdateStepOrder,
    val locationManager: LocationManager,
    val inCurrentCity: InCurrentCity,
    val stepListener: StepListener,
    val getUserReservation: GetUserReservation,
    val deleteUserReservation: DeleteUserReservation,
    val saveUserReservation: SaveUserReservation,
    val getTripianUser: GetTripianUser,
    val tripModelRepository: TripModelRepository,
    val getBookingInfo: GetBookingInfo,
    val addOffer: AddOffer,
    val removeOffer: RemoveOffer,
    val getMyOffers: GetMyOffers,
    val getMyOffersListener: GetMyOffersListener,
    val app: Application
) : BaseViewModel(
    getPlaceDetail,
    addFavorite,
    deleteFavorite,
    favoriteListener,
    addStep,
    deleteStep,
    updateStepOrder,
    inCurrentCity,
    stepListener,
    getUserReservation,
    deleteUserReservation,
    saveUserReservation,
    getBookingInfo,
    addOffer,
    removeOffer,
    getMyOffers,
    getMyOffersListener
) {

    var onSetPlaceDetailListener = MutableLiveData<PlaceDetail>()
    var onSetImagePageListener = MutableLiveData<String>()
    var onSetHourListener = MutableLiveData<OpenHour>()
    var onSetFavoriteListener = MutableLiveData<Boolean>()
    var onEnableLocationListener = MutableLiveData<Unit>()
    var onSetOwnerListener = MutableLiveData<String>()
    var onHasReservationListener = MutableLiveData<Boolean>()
    var onShowGygListener = MutableLiveData<List<ExperiencesItem>>()
    var onSetOffersListener = MutableLiveData<List<Offer>>()


    var onButterflyModeDisableListener = MutableLiveData<Unit>()
    var onShareUrlListener = MutableLiveData<String>()
    var openUrlListener = MutableLiveData<String>()

    var onSetIconListener = MutableLiveData<Mode>()

    private var imagesSize = 0
    private var isHourOpen = true
    private var hours: List<OpenHour>? = null
    private var favorite: Favorite? = null
    private var images: List<Image>? = null
    private var offers: List<Offer>? = null

    private lateinit var detail: TripDetail
    private lateinit var placeDetail: PlaceDetail

    private var mode: Mode? = null

    private var booking: PlaceBooking? = null
    private var reservationRedirect: Booking? = null

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        detail = arguments!!.getSerializable("detail") as TripDetail

        if (detail.order != -1) {
            mode = Mode.CHANGE
        }

        if (detail.butterflyMode) {
            onButterflyModeDisableListener.postValue(Unit)
        }

        showLoading()

        getPlaceDetail.on(GetPlaceDetail.Params(detail.poiId), success = {
            placeDetail = it

            hours = it.hours

            images = it.images

            images!![0].imageOwner?.title?.let { onSetOwnerListener.postValue("@$it") }

            onSetPlaceDetailListener.postValue(it)

            if (!it.images.isNullOrEmpty()) {
                imagesSize = it.images!!.size
                onSetImagePageListener.postValue("1/$imagesSize")
            }

            favorite = it.favorite
            onSetFavoriteListener.postValue(favorite != null)

            if (mode == null) {
                mode = it.mode
            }

            onSetIconListener.postValue(mode!!)

            onClickedHours()

            if (detail.butterflyMode) {
                onButterflyModeDisableListener.postValue(Unit)
            }

            getBookingInfo.on(GetBookingInfo.Params(detail.poiId), success = {
                booking = it

                booking?.getYourGuide?.let { booking ->
                    onShowGygListener.postValue(booking.products?.map { product ->
                        productToExperienceItem(
                            product
                        )
                    }?.let { it1 -> ArrayList(it1) })
                }

//                updateUserReservation()

                booking?.openTable?.let { reservationRedirect = it }
                    ?: booking?.yelp?.let { reservationRedirect = it }

                if (reservationRedirect != null) {
                    onHasReservationListener.postValue(false)
                }
            })
            offers = it.offers

            getMyOffers.on(
                GetMyOffers.Params(dateFrom = null, dateTo = null, isClear = false),
                success = { offerPois ->
                    val currentPoiOffers =
                        offerPois.firstOrNull { poi -> poi.id == detail.poiId }?.offers

                    currentPoiOffers?.forEach { myOffer ->
                        offers?.find { offer -> offer.id == myOffer.id }?.apply {
                            optIn = true
                            optInDate = myOffer.optInDate
                        }
                    }
                    onSetOffersListener.postValue(offers ?: arrayListOf())
                })
            hideLoading()
        }, error = {
            hideLoading()

            if (it.type == AlertType.DIALOG) {
                showDialog(contentText = it.errorDesc)
            } else {
                showAlert(AlertType.ERROR, it.errorDesc)
            }
        })

        favoriteListener.on(success = {
            if (it.poiId == detail.poiId) {
                favorite = it

                onSetFavoriteListener.postValue(it.isFavorite)
            }
        })

        getMyOffersListener.on(success = {
            hideLoading()
            val currentPoiOffers = it.firstOrNull { poi -> poi.id == detail.poiId }?.offers

//            currentPoiOffers?.forEach { myOffer ->
//                offers?.find { offer -> offer.id == myOffer.id }?.apply {
//                    optIn = true
//                    optInDate = myOffer.optInDate
//                }
//            }
            offers?.forEach { offer ->
                offer.optIn = false
                offer.optInDate = null
                currentPoiOffers?.firstOrNull { poiOffer -> poiOffer.id == offer.id }
                    ?.let { myOffer ->
                        offer.optIn = true
                        offer.optInDate = myOffer.optInDate
                    }

            }
            onSetOffersListener.postValue(offers ?: arrayListOf())
        }, error = {
            showAlert(AlertType.ERROR, "error my offers")
        })

        stepListener.on(success = {
            if (it.stepId == detail.stepId || it.poiId == detail.poiId) {
                // Add step durumunda step id degisiyor
                detail.stepId = it.stepId
            }
        })
    }

    override fun onResume() {
        super.onResume()

        if (!detail.butterflyMode && !isHmsOnly(app.applicationContext)) {
            locationManager.on(Unit, success = {
                // Location found
                inCurrentCity.on(InCurrentCity.Params(it), success = { enable ->
                    if (enable) onEnableLocationListener.postValue(Unit)
                })
            })
        }

//        updateUserReservation()
    }

    fun onClickedNavigation() {
        goBack()
    }

    fun onImageChanged(position: Int) {
        images!![position].imageOwner?.title?.let { onSetOwnerListener.postValue("@$it") }
        onSetImagePageListener.postValue("${position + 1}/$imagesSize")
    }

    fun onClickedHours() {
        if (hours.isNullOrEmpty()) return

        if (isHourOpen) {
            onSetHourListener.postValue(hours!!.find { it.isToday })
        } else {
            onSetHourListener.postValue(OpenHour().apply {
                hour = hours!!.map { it.hour }.joinToString(separator = "\n")
            })
        }

        isHourOpen = !isHourOpen
    }

    fun onClickedFavorite() {
        showLoading()

        if (favorite == null || (favorite != null && !favorite!!.isFavorite)) {
            addFavorite.on(AddFavorite.Params(detail.poiId), success = {
                hideLoading()
            }, error = {
                hideLoading()

                if (it.type == AlertType.DIALOG) {
                    showDialog(contentText = it.errorDesc)
                } else {
                    showAlert(AlertType.ERROR, it.errorDesc)
                }
            })
        } else {
            deleteFavorite.on(DeleteFavorite.Params(favorite!!.id!!), success = {
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

    fun onClickedAction() {
        showLoading()

        when (mode) {
            Mode.ADD -> {
                // Add state
                addStep.on(AddStep.Params(poiId = detail.poiId), success = {
                    mode = Mode.REMOVE
                    onSetIconListener.postValue(mode!!)

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
            Mode.REMOVE -> {
                // Remove state
                deleteStep.on(DeleteStep.Params(detail.stepId), success = {
                    mode = Mode.ADD
                    onSetIconListener.postValue(mode!!)

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
            Mode.CHANGE -> {
                // Change state
                updateStepOrder.on(
                    UpdateStepOrder.Params(
                        detail.stepId,
                        detail.poiId,
                        order = null
                    ), success = {
                        mode = Mode.REMOVE
                        onSetIconListener.postValue(mode!!)

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
            else -> {}
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

        if (requestCode == 1001) { // Yelp provider
//            if (resultCode == Activity.RESULT_OK) {
//                val reservationStatusModel = data!!.extras!!.getSerializable("data") as ReservationDetailModel?
//
//                val reservation = Reservation()
//                reservation.key = "YELP"
//                reservation.provider = "YELP"
//                reservation.poiId = detail.poiId
//                reservation.trip_hash = tripModelRepository.trip?.tripHash
//
//                val reservationDetail = ReservationDetail()
//                reservationDetail.reservationId = reservationStatusModel!!.reservationId
//                reservationDetail.confirmationUrl = reservationStatusModel.confirmationUrl
//                reservationDetail.notes = reservationStatusModel.notes
//
//                val reservationStatus = ReservationStatus()
//                reservationStatus.isActive = reservationStatusModel.reservationStatus!!.active
//                reservationStatus.covers = reservationStatusModel.reservationStatus!!.covers
//                reservationStatus.date = reservationStatusModel.reservationStatus!!.date
//                reservationStatus.time = reservationStatusModel.reservationStatus!!.time
//                reservationDetail.reservationStatus = reservationStatus
//                reservation.value = reservationDetail
//
//                showLoading()
//
//                saveUserReservation.on(SaveUserReservation.Params(reservation), success = {
//                    updateUserReservation()
//                }, error = {
//                    hideLoading()
//
//                    if (it.type == AlertType.DIALOG) {
//                        showDialog(contentText = it.errorDesc)
//                    } else {
//                        showAlert(AlertType.ERROR, it.errorDesc)
//                    }
//                })
//            }
        } else {
            locationManager.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        locationManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    fun onClickedRoute() {
        eventBus.post(EventMessage(EventConstants.LocationRedirect, placeDetail.mapStep))

        finishActivity()
    }

    fun onClickedOwner(pos: Int) {
        if (!TextUtils.isEmpty(images!![pos].imageOwner?.url)) {
            startActivity(ACWebPage::class, bundleOf(Pair("url", images!![pos].imageOwner?.url)))
        }
    }

    fun onClickedShare() {
        onShareUrlListener.postValue("https://trial.tripian.com/place/${detail.poiId}")
        // TODO: share url unutma!
//        onShareUrlListener.postValue("https://tripian.com/place/${detail.poiId}")
    }

    fun onClickedReservation() {
        reservationRedirect?.products?.get(0)?.url?.let { openUrlListener.postValue(it) }
    }

    fun onClickedTour(item: ExperiencesItem) {
        startActivity(ACExperienceDetail::class, Bundle().apply {
            putLong("tourId", item.id!!)
            putString("city", tripModelRepository.trip?.city?.name)
        })
    }


    fun onClickedOffer(item: Offer, claimDate: String) {
        showLoading()
        addOffer.on(AddOffer.Params(item.id, claimDate))
    }

    fun onClickedRemoveOffer(item: Offer) {
        showLoading()
        removeOffer.on(RemoveOffer.Params(item.id))
    }

    private fun productToExperienceItem(product: Product): ExperiencesItem {
        return ExperiencesItem().apply {
            id = product.id?.toLong()
            title = product.title
            rating = product.rating
            rateCount = product.ratingCount ?: 0
            price = product.price?.roundTo(1).toString()
            image = product.image
        }
    }
}