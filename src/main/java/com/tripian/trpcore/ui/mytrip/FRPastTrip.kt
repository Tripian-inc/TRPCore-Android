package com.tripian.trpcore.ui.mytrip

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tripian.one.api.trip.model.Trip
import com.tripian.trpcore.base.BaseFragment
import com.tripian.trpcore.databinding.FrMyTripBinding
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.observe

/**
 * Created by semihozkoroglu on 15.08.2020.
 */
class FRPastTrip : BaseFragment<FrMyTripBinding, FRPastTripVM>(FrMyTripBinding::inflate) {

    companion object {
        fun newInstance(): FRPastTrip {
            return FRPastTrip()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvList.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)

        binding.tvError.text = getLanguageForKey(LanguageConst.NO_PAST_TRIPS)
    }

    override fun setListeners() {
        super.setListeners()

        binding.swipeLayout.setOnRefreshListener { viewModel.onRefresh() }
    }

    override fun setReceivers() {
        super.setReceivers()

        observe(viewModel.onSetTripsListener) {
            binding.rvList.adapter =
                object : AdapterMyTrip(
                    requireContext(),
                    it!!,
                    isPastTrip = true,
                    viewModel.miscRepository
                ) {
                    override fun onClickedItem(trip: Trip) {
                        viewModel.onClickedItem(trip)
                    }

                    override fun onClickedEdit(trip: Trip) {}

                    override fun onClickedDelete(trip: Trip) {
                        viewModel.onClickedDelete(trip)
                    }
                }
        }

        observe(viewModel.onShowProgressListener) {
            binding.swipeLayout.isRefreshing = true
        }

        observe(viewModel.onHideProgressListener) {
            binding.swipeLayout.isRefreshing = false
        }

        observe(viewModel.onShowErrorListener) {
            binding.rvList.visibility = View.GONE
            binding.llError.visibility = View.VISIBLE
        }

        observe(viewModel.onHideErrorListener) {
            binding.rvList.visibility = View.VISIBLE
            binding.llError.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        binding.swipeLayout.isRefreshing = false

        super.onDestroyView()
    }
}