package com.tripian.trpcore.ui.trip

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseBottomDialogFragment
import com.tripian.trpcore.databinding.FrPoiViewBinding
import com.tripian.trpcore.domain.model.MapStep
import com.tripian.trpcore.util.extensions.observe
import com.tripian.trpcore.util.extensions.toSmallUrl

/**
 * Created by semihozkoroglu on 20.09.2020.
 */
class FRPoiView :
    BaseBottomDialogFragment<FrPoiViewBinding, FRPoiViewVM>(FrPoiViewBinding::inflate) {

    companion object {
        fun newInstance(mapStep: MapStep): FRPoiView {
            val fragment = FRPoiView()

            val data = Bundle()
            data.putSerializable("mapStep", mapStep)

            fragment.arguments = data

            return fragment
        }
    }

    override fun setListeners() {
        super.setListeners()

        binding.cvItem.setOnClickListener { viewModel.onClickedItem() }
        binding.imAction.setOnClickListener { viewModel.onClickedAction() }
    }

    override fun setReceivers() {
        super.setReceivers()

        observe(viewModel.onShowAddListener) {
            if (it!!) {
                binding.imAction.setImageResource(R.drawable.ic_plus_box_black)
            } else {
                binding.imAction.setImageResource(R.drawable.ic_minus_box_red)
            }

            binding.imAction.visibility = View.VISIBLE
        }

        observe(viewModel.onSetMapStepListener) {
            binding.tvTitle.text = it?.name

            if (TextUtils.isEmpty(it?.category)) {
                if (!TextUtils.isEmpty(it?.description)) {
                    binding.tvDescription.text = it?.description
                } else {
                    binding.tvDescription.visibility = View.GONE
                }
            } else {
                binding.tvDescription.text = it?.category
            }

            if (!TextUtils.isEmpty(it?.image)) {
                Glide.with(requireContext()).load(it?.image?.toSmallUrl())
                    .apply(RequestOptions().circleCrop())
                    .placeholder(ContextCompat.getDrawable(requireContext(), R.drawable.bg_place_holder_image))
                    .into(binding.imPoi)
            }

            if (it?.rating != -1f) {
                binding.rateBar.rating = it!!.rating
            } else {
                binding.rateBar.visibility = View.GONE
            }
        }

        observe(viewModel.onDismissListener) {
            dismiss()
        }
    }
}