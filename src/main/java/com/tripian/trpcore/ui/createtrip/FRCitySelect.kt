package com.tripian.trpcore.ui.createtrip

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tripian.one.api.cities.model.City
import com.tripian.trpcore.base.BaseBottomDialogFragment
import com.tripian.trpcore.databinding.FrCitySelectBinding
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.observe
import com.tripian.trpcore.util.extensions.showKeyboard

class FRCitySelect : BaseBottomDialogFragment<FrCitySelectBinding, FRCitySelectVM>(FrCitySelectBinding::inflate) {

    companion object {
        fun newInstance(): FRCitySelect {
            return FRCitySelect()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvList.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
    }

    override fun setListeners() {
        super.setListeners()
        binding.etSearch.hint = getLanguageForKey(LanguageConst.SEARCH)
        binding.etSearch.doAfterTextChanged { viewModel.onSearchEntered(it.toString()) }
        binding.etSearch.showKeyboard()
    }

    override fun setReceivers() {
        super.setReceivers()

        observe(viewModel.onSetCityListener) {
//            if (rvList.adapter == null || it!!.first) {
            binding.rvList.adapter = object : AdapterSelectCity(requireContext(), it!!.second!!) {
                override fun onClickedItem(city: City) {
                    viewModel.onSelectedCity(city)
                }
            }
//            } else {
//                rvList.adapter?.notifyDataSetChanged()
//            }
        }

        observe(viewModel.onDismissListener) {
            dismiss()
        }
    }
}