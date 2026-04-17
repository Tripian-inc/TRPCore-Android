package com.tripian.trpcore.ui.timeline

import android.os.Bundle
import android.view.View
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseSimpleBottomSheet
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.BottomSheetTimeSelectionBinding
import com.tripian.trpcore.ui.timeline.addplan.MaterialTimePickerHelper
import com.tripian.trpcore.ui.timeline.addplan.showComposeTimePicker
import com.tripian.trpcore.util.LanguageConst

/**
 * Bottom sheet for time range selection (start + end time)
 * Uses ComposeTimePickerDialog for individual time selection
 *
 * Replaces deprecated TimePickerBottomSheet
 */
class TimeSelectionBottomSheet : BaseSimpleBottomSheet<BottomSheetTimeSelectionBinding>(
    BottomSheetTimeSelectionBinding::inflate
) {
    // State
    private var startTime: String? = null  // Format: "HH:mm"
    private var endTime: String? = null    // Format: "HH:mm"

    // Callback
    private var onTimeSelectedListener: ((startTime: String?, endTime: String?) -> Unit)? = null

    override fun getTheme(): Int = R.style.TrpTimelineBottomSheetDialog

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Restore from arguments
        arguments?.let { args ->
            startTime = args.getString(ARG_START_TIME)
            endTime = args.getString(ARG_END_TIME)
        }

        setupLabels()
        updateTimeDisplays()
        setupListeners()
    }

    private fun setupLabels() {
        // Title
        binding.tvTitle.text = TRPCore.core.miscRepository
            .getLanguageValueForKey(LanguageConst.ADD_PLAN_TIME)  // "Cambiar hora"

        // Start time label
        binding.tvStartTimeLabel.text = TRPCore.core.miscRepository
            .getLanguageValueForKey(LanguageConst.ADD_PLAN_START_TIME)  // "Hora de inicio"

        // End time label
        binding.tvEndTimeLabel.text = TRPCore.core.miscRepository
            .getLanguageValueForKey(LanguageConst.ADD_PLAN_END_TIME)  // "Hora de finalización"

        // Confirm button
        binding.btnConfirm.text = TRPCore.core.miscRepository
            .getLanguageValueForKey(LanguageConst.ADD_PLAN_CONFIRM)  // "Confirmar"
    }

    private fun updateTimeDisplays() {
        // Start time
        val selectText = TRPCore.core.miscRepository
            .getLanguageValueForKey(LanguageConst.ADD_PLAN_SELECT)

        binding.tvStartTime.text = startTime?.let {
            MaterialTimePickerHelper.formatTo12h(it)
        } ?: selectText

        binding.tvStartTime.setTextColor(
            requireContext().getColor(
                if (startTime != null) R.color.trp_text_primary
                else R.color.trp_fgWeak
            )
        )

        // End time
        binding.tvEndTime.text = endTime?.let {
            MaterialTimePickerHelper.formatTo12h(it)
        } ?: selectText

        binding.tvEndTime.setTextColor(
            requireContext().getColor(
                if (endTime != null) R.color.trp_text_primary
                else R.color.trp_fgWeak
            )
        )
    }

    private fun setupListeners() {
        // Start time field click
        binding.llStartTime.setOnClickListener {
            showStartTimePicker()
        }

        // End time field click
        binding.llEndTime.setOnClickListener {
            showEndTimePicker()
        }

        // Confirm button
        binding.btnConfirm.setOnClickListener {
            onTimeSelectedListener?.invoke(startTime, endTime)
            dismiss()
        }
    }

    private fun showStartTimePicker() {
        showComposeTimePicker(
            initialTime = startTime,
            onTimeSelected = { hour, minute ->
                val time24h = MaterialTimePickerHelper.formatTo24h(hour, minute)
                startTime = time24h

                // Clear end time if it's now invalid (before new start time)
                if (endTime != null && !MaterialTimePickerHelper.isEndTimeAfterStartTime(time24h, endTime)) {
                    endTime = null
                }

                updateTimeDisplays()
            }
        )
    }

    private fun showEndTimePicker() {
        // Validate start time is selected first
        if (startTime == null) {
            showAlert(LanguageConst.ADD_PLAN_SELECT_START_TIME_FIRST)
            return
        }

        showComposeTimePicker(
            initialTime = endTime ?: startTime,  // If no end time, default to start time
            minTime = startTime,  // Minimum selectable time is start time
            onTimeSelected = { hour, minute ->
                val time24h = MaterialTimePickerHelper.formatTo24h(hour, minute)
                endTime = time24h
                updateTimeDisplays()
            }
        )
    }

    private fun showAlert(languageKey: String) {
        val message = TRPCore.core.miscRepository.getLanguageValueForKey(languageKey)
        android.app.AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    fun setOnTimeSelectedListener(listener: (startTime: String?, endTime: String?) -> Unit) {
        onTimeSelectedListener = listener
    }

    companion object {
        const val TAG = "TimeSelectionBottomSheet"

        private const val ARG_START_TIME = "start_time"
        private const val ARG_END_TIME = "end_time"

        fun newInstance(
            startTime: String? = null,
            endTime: String? = null
        ): TimeSelectionBottomSheet {
            return TimeSelectionBottomSheet().apply {
                arguments = Bundle().apply {
                    startTime?.let { putString(ARG_START_TIME, it) }
                    endTime?.let { putString(ARG_END_TIME, it) }
                }
            }
        }
    }
}
