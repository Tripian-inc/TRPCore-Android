package com.tripian.trpcore.ui.trip

import com.tripian.trpcore.base.BaseDialogFragment
import com.tripian.trpcore.databinding.FrChangeTimeBinding
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.observe

/**
 * Created by semihozkoroglu on 20.09.2020.
 */
class FRChangeTime : BaseDialogFragment<FrChangeTimeBinding, FRChangeTimeVM>(FrChangeTimeBinding::inflate) {

    companion object {
        fun newInstance(): FRChangeTime {
            return FRChangeTime()
        }
    }

    override fun setListeners() {
        super.setListeners()

        binding.tvStartTime.setOnClickListener { viewModel.onClickedStartTime() }
        binding.tvEndTime.setOnClickListener { viewModel.onClickedEndTime() }
        binding.btnNegative.setOnClickListener { dismiss() }
        binding.btnPositive.setOnClickListener { viewModel.onClickedOk() }
        binding.btnPositive.text = getLanguageForKey(LanguageConst.UPDATE)
    }

    override fun setReceivers() {
        super.setReceivers()

        observe(viewModel.onSetStartTimeListener) {
            binding.tvStartTime.text = it
        }

        observe(viewModel.onSetEndTimeListener) {
            binding.tvEndTime.text = it
        }

        observe(viewModel.onDismissListener) {
            dismiss()
        }
    }
}