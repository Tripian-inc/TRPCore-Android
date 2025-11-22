package com.tripian.trpcore.ui.createtrip

import android.text.TextUtils
import androidx.core.view.isVisible
import com.google.android.material.chip.Chip
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseFragment
import com.tripian.trpcore.databinding.FrCreateTripTravelerInfoBinding
import com.tripian.trpcore.util.CreateTripSteps
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.ToolbarProperties
import com.tripian.trpcore.util.extensions.observe

/**
 * Created by cemcaygoz on 10.01.2023.
 */
class FRCreateTripTravelerInfo : BaseFragment<FrCreateTripTravelerInfoBinding, FRCreateTripTravelerInfoVM>(FrCreateTripTravelerInfoBinding::inflate) {

    companion object {
        fun newInstance(): FRCreateTripTravelerInfo {
            return FRCreateTripTravelerInfo()
        }
    }

    override fun getToolbarProperties(): ToolbarProperties {
        return ToolbarProperties(createTripStep = CreateTripSteps.TRAVELER_INFO)
    }

    override fun setListeners() {
        super.setListeners()

        binding.apply {
            tvTraveler.text = viewModel.getLanguageForKey(LanguageConst.TRAVELER_INFO)
            tvAdults.text = viewModel.getLanguageForKey(LanguageConst.ADULTS)
            tvChildren.text = viewModel.getLanguageForKey(LanguageConst.CHILDREN)
            tvWhereWillStay.text = viewModel.getLanguageForKey(LanguageConst.WHERE_START)
            tvHotelPlaceholder.text = viewModel.getLanguageForKey(LanguageConst.HOTEL_PLACEHOLDER)
            tvWhoTravelWith.text = viewModel.getLanguageForKey(LanguageConst.WHO_TRAVEL_WITH)
            tvCompanionPlaceholder.text = viewModel.getLanguageForKey(LanguageConst.ADD_COMPANION)
            tvCreateCompanion.text = viewModel.getLanguageForKey(LanguageConst.CREATE_COMPANION_PROFILE)
        }


        binding.imAdultMinus.setOnClickListener { viewModel.onClickedMinusAdult() }
        binding.imAdultPlus.setOnClickListener { viewModel.onClickedPlusAdult() }
        binding.imChildMinus.setOnClickListener { viewModel.onClickedMinusChild() }
        binding.imChildPlus.setOnClickListener { viewModel.onClickedPlusChild() }

        binding.llHotel.setOnClickListener { viewModel.onClickedSearch() }

        binding.llSelectCompanion.setOnClickListener { viewModel.onClickedCompanionSelect() }
        binding.llCreateCompanion.setOnClickListener { viewModel.onClickedCompanionCreate() }
    }

    override fun setReceivers() {
        super.setReceivers()

        observe(viewModel.onSetAdultListener) {
            binding.imAdultMinus.alpha = if (it == "1") 0.5f else 1f
            binding.imAdultPlus.alpha = if (it == "99") 0.5f else 1f
            binding.tvAdultCount.text = it
        }

        observe(viewModel.onSetChildListener) {
            binding.imChildMinus.alpha = if (it == "0") 0.5f else 1f
            binding.imChildPlus.alpha = if (it == "99") 0.5f else 1f
            binding.tvChildCount.text = it
        }

        observe(viewModel.onSetPlaceListener) {
            if (TextUtils.isEmpty(it)) {
                binding.tvHotel.isVisible = false
                binding.tvHotelPlaceholder.isVisible = true
            } else {
                binding.tvHotel.isVisible = true
                binding.tvHotelPlaceholder.isVisible = false

                binding.tvHotel.text = it
            }
        }

        observe(viewModel.onSetCompanionListener) {
            binding.tagCompanionGroup.removeAllViews()
            binding.tagCompanionGroup.isVisible = !it.isNullOrEmpty()
            it?.let { companionNames ->
                setTag(companionNames)
            }
        }

    }

    private fun setTag(tagList: List<String>) {
        for (index in tagList.indices) {
            val tagName = tagList[index]
            val chip = layoutInflater.inflate(R.layout.item_companion_select_chip, container, false) as Chip
            chip.text = tagName
            chip.setOnCloseIconClickListener {
                viewModel.oncClickedCompanionRemove(companionName = tagName)
            }
            binding.tagCompanionGroup.addView(chip)
        }
    }

}