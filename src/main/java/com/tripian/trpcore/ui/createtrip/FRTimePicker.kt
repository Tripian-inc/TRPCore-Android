package com.tripian.trpcore.ui.createtrip

import android.os.Bundle
import android.view.View
import com.tripian.trpcore.base.BaseBottomDialogFragment
import com.tripian.trpcore.databinding.FrTimePickerBinding
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.getHoursForTime

/**
 * Created by semihozkoroglu on 27.08.2020.
 */
class FRTimePicker :
    BaseBottomDialogFragment<FrTimePickerBinding, FRTimePickerVM>(FrTimePickerBinding::inflate) {

    private lateinit var subList: List<String>
    private lateinit var allHours: List<String>

    companion object {
        fun newInstance(
            tag: String,
            buttonText: String,
            initTime: String? = null,
            minTime: String? = null,
            maxTime: String? = null
        ): FRTimePicker {
            val fragment = FRTimePicker()

            val data = Bundle()
            data.putString("tag", tag)
            data.putString("buttonText", buttonText)

            initTime?.let { data.putString("initTime", initTime) }
            minTime?.let { data.putString("minTime", minTime) }
            maxTime?.let { data.putString("maxTime", maxTime) }

            fragment.arguments = data

            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val time = arguments?.getString("initTime") ?: "09:00"
        val minTime = arguments?.getString("minTime") ?: "00:00"
        val maxTime = arguments?.getString("maxTime") ?: "23:30"

        binding.npTimes.minValue = 0
        binding.npTimes.wrapSelectorWheel = false

        allHours = getHoursForTime(interval = 30)

        val displayedValues = arrayListOf<String>()
        var startIndex = allHours.indexOf(minTime)
        var endIndex = allHours.indexOf(maxTime)
        if (startIndex < 0) {
            startIndex = 0
        }
        if (endIndex < startIndex) {
            endIndex = allHours.size - 1
        }

        subList = allHours.subList(startIndex, endIndex + 1)
        displayedValues.addAll(subList)

        binding.npTimes.maxValue = displayedValues.size - 1
        binding.npTimes.value = if (subList.indexOf(time) != -1) {
            subList.indexOf(time)
        } else {
            0
        }
        binding.npTimes.displayedValues = displayedValues.toTypedArray()

        val buttonText = requireArguments().getString("buttonText")
        if (!buttonText.isNullOrEmpty()) {
            binding.btnApply.text = buttonText
        }
    }

    override fun setListeners() {
        super.setListeners()
        binding.tvTitle.text = getLanguageForKey(LanguageConst.SELECT_HOURS)
        binding.btnApply.text = getLanguageForKey(LanguageConst.APPLY)
        binding.btnApply.setOnClickListener { viewModel.onClickedOk(subList[binding.npTimes.value]) }
    }
}