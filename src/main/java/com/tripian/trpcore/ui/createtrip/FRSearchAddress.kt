package com.tripian.trpcore.ui.createtrip

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tripian.one.api.cities.model.City
import com.tripian.trpcore.base.BaseBottomDialogFragment
import com.tripian.trpcore.databinding.FrSearchAddressBinding
import com.tripian.trpcore.domain.model.PlaceAutocomplete
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.hideKeyboard
import com.tripian.trpcore.util.extensions.observe
import com.tripian.trpcore.util.extensions.showKeyboard

/**
 * Created by cemcaygoz on 10.01.2023.
 */
class FRSearchAddress : BaseBottomDialogFragment<FrSearchAddressBinding, FRSearchAddressVM>(FrSearchAddressBinding::inflate) {

    companion object {
        fun newInstance(city: City): FRSearchAddress {
            val fragment = FRSearchAddress()

            val data = Bundle()
            data.putSerializable("city", city)

            fragment.arguments = data

            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvList.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
    }

    override fun setListeners() {
        binding.etSearch.showKeyboard()
        binding.tvError.text = getLanguageForKey(LanguageConst.ENTER_START)
        binding.etSearch.hint = getLanguageForKey(LanguageConst.SEARCH)
        binding.etSearch.doAfterTextChanged { viewModel.find(it.toString()) }
    }

    override fun setReceivers() {
        observe(viewModel.onSetPlacesListener) {
            binding.tvError.isVisible = it.isNullOrEmpty()

            binding.rvList.adapter = object : AdapterSelectPlace(requireContext(), it!!) {
                override fun onClickedItem(place: PlaceAutocomplete) {
                    binding.etSearch.hideKeyboard()

                    viewModel.onClickedItem(place)
                }
            }
        }

        observe(viewModel.onShowProgressListener) {
            binding.pbProgress.visibility = View.VISIBLE
        }

        observe(viewModel.onHideProgressListener) {
            binding.pbProgress.visibility = View.GONE
        }
    }
}