package com.tripian.trpcore.ui.createtrip

import androidx.core.view.isVisible
import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.databinding.AcCreateTripBinding
import com.tripian.trpcore.util.ToolbarProperties

/**
 * Created by semihozkoroglu on 21.08.2020.
 */
class ACCreateTrip : BaseActivity<AcCreateTripBinding, ACCreateTripVM>() {

    override fun getViewBinding(): AcCreateTripBinding {
        return AcCreateTripBinding.inflate(layoutInflater)
    }

    override fun setToolbarProperties(properties: ToolbarProperties) {
        super.setToolbarProperties(properties)

        properties.createTripStep?.let { step ->
            binding.imPrevArrow.isVisible = step.isPrevArrowVisible()
            binding.tvPrevTitle.isVisible = step.isPrevArrowVisible()

            binding.imNextArrow.isVisible = step.isNextArrowVisible()
            binding.tvNextTitle.isVisible = step.isNextArrowVisible()

            binding.tvPrevTitle.text = getLanguageForKey(step.getPrevTitle())
            binding.tvNextTitle.text = getLanguageForKey(step.getNextTitle())
            binding.tvTitle.text = getLanguageForKey(step.getCurrentTitle())
            binding.btnNext.text = getLanguageForKey(step.nextButtonTitle())

            viewModel.onSetCurrentPage(step)
        }

        binding.appBarLayout.setExpanded(true, true)
    }

    override fun setListeners() {
        binding.imNavigation.setOnClickListener { viewModel.onClickedBack() }
        binding.btnNext.setOnClickListener { viewModel.onClickedNext() }
        binding.imClose.setOnClickListener { finish() }
    }

    override fun setReceivers() {
    }
}