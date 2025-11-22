package com.tripian.trpcore.ui.trip

import android.os.Bundle
import android.view.View
import com.tripian.trpcore.base.BaseBottomDialogFragment
import com.tripian.trpcore.databinding.FrChangeTimePickerBinding
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.getHoursForTime

class FRChangeTimePicker :
    BaseBottomDialogFragment<FrChangeTimePickerBinding, FRChangeTimePickerVM>(
        FrChangeTimePickerBinding::inflate
    ) {

    private lateinit var allHours: List<String>
    private var subListForEndTime: List<String> = arrayListOf()
    private lateinit var initStartTime: String
    private lateinit var initEndTime: String
    private var estimatedDuration: Int = 30

    companion object {
        fun newInstance(
            stepId: Int? = null,
            startTime: String? = null,
            endTime: String? = null,
            estimatedDuration: Int? = null,
        ): FRChangeTimePicker {
            val fragment = FRChangeTimePicker()

            val data = Bundle()

            startTime?.let { data.putString("startTime", startTime) }
            endTime?.let { data.putString("endTime", endTime) }
            estimatedDuration?.let { data.putInt("estimatedDuration", estimatedDuration) }
            stepId?.let { data.putInt("stepId", stepId) }

            fragment.arguments = data

            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initStartTime = arguments?.getString("startTime") ?: "09:00"
        initEndTime = arguments?.getString("endTime") ?: "21:00"
        estimatedDuration = arguments?.getInt("estimatedDuration") ?: 10
        viewModel.stepId = arguments?.getInt("stepId")

        binding.tvTitle.text =
            viewModel.getLanguageForKey("trips.myTrips.itinerary.customPoiModal.visitTime.title")
        binding.btnApply.text =
            viewModel.getLanguageForKey(LanguageConst.APPLY)
        binding.tvEstimatedText.text =
            viewModel.getLanguageForKey(LanguageConst.ESTIMATED_DURATION)

        binding.tvEstimated.text = "$estimatedDuration"

        binding.npStartTimes.minValue = 0
        binding.npStartTimes.wrapSelectorWheel = false
        binding.npEndTimes.minValue = 0
        binding.npEndTimes.wrapSelectorWheel = false

        allHours = getHoursForTime(interval = 5)

        binding.npStartTimes.maxValue = allHours.size - 1
        binding.npStartTimes.value = getStartTimeIndex(initStartTime)
        binding.npStartTimes.displayedValues = allHours.toTypedArray()
        binding.npStartTimes.setOnValueChangedListener { picker, oldVal, newVal ->

            //Display the newly selected number to text view
            setEndTimePicker(allHours[newVal], isFirstInit = false)
        }
        setEndTimePicker(initStartTime)
    }

    private fun getStartTimeIndex(time: String): Int {

        var startIndex = allHours.indexOf(time)
        if (startIndex < 0) {
            startIndex = 0
        }
        return startIndex
    }

    private fun setEndTimePicker(startTime: String, isFirstInit: Boolean = true) {

        val displayedValues = arrayListOf<String>()
        val startTimeIndex = getStartTimeIndex(startTime)

        subListForEndTime = allHours.subList(startTimeIndex, allHours.size)
        displayedValues.addAll(subListForEndTime)

        val endTimeIndex = if (isFirstInit) {
            if (subListForEndTime.indexOf(initEndTime) != -1) {
                subListForEndTime.indexOf(initEndTime)
            } else {
                0
            }
        } else {
            if ((estimatedDuration / 5) >= displayedValues.size) {
                displayedValues.size - 1
            } else {
                (estimatedDuration / 5)
            }

        }
        binding.npEndTimes.displayedValues = null
        binding.npEndTimes.maxValue = displayedValues.size - 1
        binding.npEndTimes.displayedValues = displayedValues.toTypedArray()
        binding.npEndTimes.value = endTimeIndex
    }

    override fun setListeners() {
        super.setListeners()

        binding.btnApply.setOnClickListener {
            viewModel.onClickedOk(
                allHours[binding.npStartTimes.value],
                subListForEndTime[binding.npEndTimes.value]
            )
        }
    }
}