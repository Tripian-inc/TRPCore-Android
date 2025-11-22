package com.tripian.trpcore.ui.trip.places

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.base.BaseFragment
import com.tripian.trpcore.databinding.FrPlacesBinding
import com.tripian.trpcore.domain.model.PlaceItem
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.Navigation
import com.tripian.trpcore.util.RecyclerViewScrollListener
import com.tripian.trpcore.util.ToolbarProperties
import com.tripian.trpcore.util.extensions.observe

/**
 * Created by semihozkoroglu on 30.09.2020.
 */
class FRPlaces : BaseFragment<FrPlacesBinding, FRPlacesVM>(FrPlacesBinding::inflate) {

    private lateinit var layoutManager: LinearLayoutManager

    companion object {
        fun newInstance(place: Place): FRPlaces {
            val fragment = FRPlaces()

            val data = Bundle()
            data.putSerializable("place", place)

            fragment.arguments = data

            return fragment
        }
    }

    override fun getToolbarProperties(): ToolbarProperties {
        return ToolbarProperties(
            title = getLanguageForKey(LanguageConst.PLACES),
            type = Navigation.CLOSE
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)

        super.onViewCreated(view, savedInstanceState)

        binding.rvList.layoutManager = layoutManager
    }

    override fun setListeners() {
        super.setListeners()
        binding.tvError.text = getLanguageForKey(LanguageConst.NO_RESULT_FOUND)

        binding.rvList.addOnScrollListener(object : RecyclerViewScrollListener(layoutManager) {
            override fun isLastPage(): Boolean {
                return viewModel.isLastPage()
            }

            override fun loadMoreItems() {
                viewModel.loadMoreItems()
            }

            override fun isLoading(): Boolean {
                return viewModel.isLoading()
            }
        })
    }

    override fun setReceivers() {
        super.setReceivers()

        observe(viewModel.onSetPlaceListener) {
            if (binding.rvList.adapter == null || it!!.first) {
                binding.rvList.adapter = object : AdapterPlace(
                    requireContext(),
                    it!!.second,
                    miscRepository = viewModel.miscRepository
                ) {
                    override fun onClickedPlace(place: PlaceItem) {
                        viewModel.onClickedPlace(place)
                    }
                }
            } else {
                binding.rvList.adapter?.notifyDataSetChanged()
            }
        }

        observe(viewModel.onSetSearchPlaceListener) {
            binding.rvList.adapter = object : AdapterPlace(
                requireContext(), it!!,
                miscRepository = viewModel.miscRepository
            ) {
                override fun onClickedPlace(place: PlaceItem) {
                    viewModel.onClickedPlace(place)
                }
            }
        }

        observe(viewModel.onShowProgressListener) {
            (activity as ACPlaces).showSearchProgress()
        }

        observe(viewModel.onHideProgressListener) {
            (activity as ACPlaces).hideSearchProgress()
        }

        observe(viewModel.onShowErrorListener) {
            binding.tvError.visibility = View.VISIBLE
        }

        observe(viewModel.onHideErrorListener) {
            binding.tvError.visibility = View.GONE
        }
    }
}