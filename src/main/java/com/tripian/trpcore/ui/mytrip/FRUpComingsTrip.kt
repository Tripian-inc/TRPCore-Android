package com.tripian.trpcore.ui.mytrip

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tripian.one.api.trip.model.Trip
import com.tripian.trpcore.base.BaseFragment
import com.tripian.trpcore.databinding.FrMyTripBinding
import com.tripian.trpcore.util.extensions.observe

/**
 * Created by semihozkoroglu on 15.08.2020.
 */
class FRUpComingsTrip : BaseFragment<FrMyTripBinding, FRUpComingsVM>(FrMyTripBinding::inflate) {

    companion object {
        fun newInstance(): FRUpComingsTrip {
            return FRUpComingsTrip()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvList.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)

        val errorText =
            "${viewModel.getLanguageForKey("trips.myTrips.upComingTrips.emptyMessage")}\n${
                viewModel.getLanguageForKey("trips.createNewTrip.title")
            } +"
        binding.tvError.text = errorText
    }

    override fun setListeners() {
        super.setListeners()

        binding.swipeLayout.setOnRefreshListener { viewModel.onRefresh() }
        binding.llError.setOnClickListener { viewModel.onClickedCreate() }
    }

    override fun setReceivers() {
        super.setReceivers()

        observe(viewModel.onSetTripsListener) {
            binding.rvList.adapter = object :
                AdapterMyTrip(requireContext(), it!!, miscRepository = viewModel.miscRepository) {
                override fun onClickedItem(trip: Trip) {
                    viewModel.onClickedItem(trip)
                }

                override fun onClickedEdit(trip: Trip) {
                    viewModel.onClickedEdit(trip)
                }

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