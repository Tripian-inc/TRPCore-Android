package com.tripian.trpcore.ui.trip

import com.tripian.trpcore.base.BaseBottomDialogFragment
import com.tripian.trpcore.databinding.FrSearchCategoryBinding
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.observe

/**
 * Created by semihozkoroglu on 19.08.2020.
 */
class FRSearchCategory :
    BaseBottomDialogFragment<FrSearchCategoryBinding, FRSearchCategoryVM>(FrSearchCategoryBinding::inflate) {

    companion object {
        fun newInstance(): FRSearchCategory {
            return FRSearchCategory()
        }
    }

    override fun setListeners() {
        super.setListeners()

        binding.tvRestaurant.text = getLanguageForKey(LanguageConst.RESTAURANTS)
        binding.tvCafes.text = getLanguageForKey(LanguageConst.CAFES)
        binding.tvAttractions.text = getLanguageForKey(LanguageConst.ATTRACTIONS)
        binding.tvAll.text = getLanguageForKey(LanguageConst.ALL)

        binding.llRestaurants.setOnClickListener { viewModel.onClickedItem(3) }
        binding.llCafes.setOnClickListener { viewModel.onClickedItem(24) }
        binding.llAttractions.setOnClickListener { viewModel.onClickedItem(1) }
        binding.llAll.setOnClickListener { viewModel.onClickedItem(999) }
    }

    override fun setReceivers() {
        super.setReceivers()

        observe(viewModel.onDismissListener) {
            dismiss()
        }
    }
}