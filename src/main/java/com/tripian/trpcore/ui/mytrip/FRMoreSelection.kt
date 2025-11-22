package com.tripian.trpcore.ui.mytrip

import android.os.Bundle
import android.view.View
import com.tripian.one.api.trip.model.Trip
import com.tripian.trpcore.base.BaseBottomDialogFragment
import com.tripian.trpcore.databinding.FrMoreSelectionBinding
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.observe

/**
 * Created by semihozkoroglu on 19.08.2020.
 */
class FRMoreSelection :
    BaseBottomDialogFragment<FrMoreSelectionBinding, FRMoreSelectionVM>(FrMoreSelectionBinding::inflate) {

    companion object {
        fun newInstance(trip: Trip, editEnable: Boolean = true): FRMoreSelection {
            val fragment = FRMoreSelection()

            val data = Bundle()
            data.putSerializable("trip", trip)
            data.putBoolean("editEnable", editEnable)

            fragment.arguments = data

            return fragment
        }
    }

    override fun setListeners() {
        super.setListeners()

        binding.tvEdit.text = getLanguageForKey(LanguageConst.EDIT_TRIP)
        binding.tvDelete.text = getLanguageForKey(LanguageConst.DELETE_TRIP)
        binding.btnCancel.text = getLanguageForKey(LanguageConst.CANCEL)

        binding.tvEdit.setOnClickListener { viewModel.onClickedEdit() }
        binding.tvDelete.setOnClickListener { viewModel.onClickedDelete() }
        binding.btnCancel.setOnClickListener { dismiss() }
    }

    override fun setReceivers() {
        super.setReceivers()

        observe(viewModel.onSetTitleListener) {
            binding.tvTitle.text = it?.first
            binding.tvDate.text = it?.second
        }

        observe(viewModel.onDismissListener) {
            dismiss()
        }

        observe(viewModel.onEditDisableListener) {
            binding.tvEdit.visibility = View.GONE
        }
    }
}