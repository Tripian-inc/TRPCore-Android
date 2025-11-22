package com.tripian.trpcore.ui.butterfly

import android.os.Bundle
import com.tripian.trpcore.base.BaseBottomDialogFragment
import com.tripian.trpcore.databinding.FrTellUsBinding
import com.tripian.trpcore.domain.model.ButterflyItem
import com.tripian.trpcore.util.extensions.observe

/**
 * Created by semihozkoroglu on 19.08.2020.
 */
class FRTellUs : BaseBottomDialogFragment<FrTellUsBinding, FRTellUsVM>(FrTellUsBinding::inflate) {

    companion object {
        fun newInstance(item: ButterflyItem): FRTellUs {
            val fragment = FRTellUs()

            val data = Bundle()
            data.putSerializable("item", item)

            fragment.arguments = data

            return fragment
        }
    }

    override fun setListeners() {
        super.setListeners()

        binding.tvVisitedBefore.setOnClickListener { viewModel.onClickedVisitedBefore() }
        binding.tvNotLike.setOnClickListener { viewModel.onClickedNotLike() }
    }

    override fun setReceivers() {
        super.setReceivers()

        observe(viewModel.onDismissListener) {
            dismiss()
        }
    }
}