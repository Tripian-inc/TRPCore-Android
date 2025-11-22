package com.tripian.trpcore.ui.trip.booking

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.DeleteUserReservation
import com.tripian.trpcore.domain.GetBooking
import com.tripian.trpcore.domain.model.PlaceItem
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.extensions.goBack
import com.tripian.trpcore.util.extensions.hideLoading
import com.tripian.trpcore.util.extensions.remove
import com.tripian.trpcore.util.extensions.showLoading
import com.tripian.gyg.repository.ExperienceRepository
import com.tripian.one.api.bookings.model.Reservation
import com.tripian.trpcore.ui.common.ACWebPage
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

class ACBookingVM @Inject constructor(
    val getBookingPlace: GetBooking,
    val deleteUserReservation: DeleteUserReservation
) : BaseViewModel(getBookingPlace, deleteUserReservation) {

    val onSetPlaceListener = MutableLiveData<List<Reservation>>()
    private var cityId: Int? = null

    private var places: ArrayList<Reservation> = arrayListOf()

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        cityId = arguments?.getInt("cityId")

        updateBookings()
    }

    override fun onResume() {
        super.onResume()

        updateBookings()
    }

    private fun updateBookings() {
        showLoading()

        getBookingPlace.on(GetBooking.Params(cityId), success = {
            it?.let { places.addAll(it) }

            onSetPlaceListener.postValue(places)

            hideLoading()
        }, error = {
            hideLoading()

            if (it.type == AlertType.DIALOG) {
                showDialog(contentText = it.errorDesc)
            } else {
                showAlert(it.type, it.errorDesc)
            }
        })
    }

    fun onClickedBack() {
        goBack()
    }

    fun onClickedPlace(reservation: Reservation) {
        reservation.value?.data?.shoppingCart?.bookings?.firstOrNull()?.ticket?.ticketUrl?.let { ticketUrl ->
            if (ticketUrl.isNotEmpty()) {
                startActivity(
                    ACWebPage::class,
                    bundleOf(Pair("url", ticketUrl))
                )
            }
        }
    }

    fun onClickedCancel(reservation: Reservation) {
        showLoading()

        reservation.value?.data?.shoppingCart?.bookingHash?.let {
            viewModelScope.launch(CoroutineExceptionHandler { _, e ->
                deleteReservation(reservationId = reservation.id)
            }) {
                ExperienceRepository.deleteBooking(it, "en")
                    .catch { deleteReservation(reservationId = reservation.id) }
                    .collect {
                        deleteReservation(reservationId = reservation.id)
                    }
            }
        }
    }

    private fun deleteReservation(reservationId: Int?) {
        reservationId?.let {
            deleteUserReservation.on(DeleteUserReservation.Params(reservationId), success = {
                places.remove { it.id == reservationId }

                onSetPlaceListener.postValue(places)
                hideLoading()
            }, error = {
                hideLoading()

                if (it.type == AlertType.DIALOG) {
                    showDialog(contentText = it.errorDesc)
                } else {
                    showAlert(it.type, it.errorDesc)
                }
            })
        }
    }
}