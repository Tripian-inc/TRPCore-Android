package com.tripian.trpcore.ui.trip.booking

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tripian.one.api.bookings.model.Reservation
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.databinding.AcBookingBinding
import com.tripian.trpcore.util.extensions.observe

/**
 * Created by semihozkoroglu on 3.10.2020.
 */
class ACBooking : BaseActivity<AcBookingBinding, ACBookingVM>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        overridePendingTransition(R.anim.anim_slide_in_up, R.anim.anim_slide_out_up)

        binding.rvList.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
    }

    override fun onPause() {
        super.onPause()

        overridePendingTransition(R.anim.anim_slide_in_down, R.anim.anim_slide_out_down)
    }

    override fun getViewBinding(): AcBookingBinding {
        return AcBookingBinding.inflate(layoutInflater)
    }

    override fun setListeners() {
        binding.tvTitle.text = viewModel.getLanguageForKey("trips.myTrips.itinerary.bookings.title")
        binding.tvError.text =
            viewModel.getLanguageForKey("trips.myTrips.itinerary.bookings.emptyMessage")
        binding.imNavigation.setOnClickListener { viewModel.onClickedBack() }
    }

    override fun setReceivers() {
        observe(viewModel.onSetPlaceListener) {
            if (it.isNullOrEmpty()) {
                binding.tvError.visibility = View.VISIBLE
                binding.rvList.visibility = View.GONE
            } else {
                binding.tvError.visibility = View.GONE
                binding.rvList.visibility = View.VISIBLE

                if (binding.rvList.adapter == null) {
                    binding.rvList.adapter = object :
                        AdapterBooking(this, it, miscRepository = viewModel.miscRepository) {
                        override fun onClickedPlace(reservation: Reservation) {
                            viewModel.onClickedPlace(reservation)
                        }

                        override fun onClickedCancel(reservation: Reservation) {
                            viewModel.onClickedCancel(reservation)
                        }
                    }
                } else {
                    binding.rvList.adapter?.notifyDataSetChanged()
                }
            }
        }
    }
}