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
 */
class TimeSelectionBottomSheet : BaseSimpleBottomSheet<BottomSheetTimeSelectionBinding>(
    BottomSheetTimeSelectionBinding::inflate
) {
    private var startTime: String? = null  // Format: "HH:mm"
    private var endTime: String? = null    // Format: "HH:mm"
    // Earliest selectable "HH:mm" for the edited item's day in its city timezone
    // (null = no floor / future day). Blocks moving an activity into the past.
    private var minTime: String? = null
    // Suggested "HH:mm" the start-time picker opens on when [startTime] is null;
    // never a restriction, purely a prefill (see CityTimeZones.defaultStartTime).
    private var defaultStartTime: String? = null

    private var onTimeSelectedListener: ((startTime: String?, endTime: String?) -> Unit)? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let { args ->
            startTime = args.getString(ARG_START_TIME)
            endTime = args.getString(ARG_END_TIME)
            minTime = args.getString(ARG_MIN_TIME)
            defaultStartTime = args.getString(ARG_DEFAULT_START_TIME)
        }

        setupLabels()
        updateTimeDisplays()
        setupListeners()
    }

    private fun setupLabels() {
        binding.tvTitle.text = TRPCore.core.miscRepository
            .getLanguageValueForKey(LanguageConst.ADD_PLAN_TIME)

        binding.tvStartTimeLabel.text = TRPCore.core.miscRepository
            .getLanguageValueForKey(LanguageConst.ADD_PLAN_START_TIME)

        binding.tvEndTimeLabel.text = TRPCore.core.miscRepository
            .getLanguageValueForKey(LanguageConst.ADD_PLAN_END_TIME)

        binding.btnConfirm.text = TRPCore.core.miscRepository
            .getLanguageValueForKey(LanguageConst.ADD_PLAN_CONFIRM)
    }

    private fun updateTimeDisplays() {
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

        binding.tvEndTime.text = endTime?.let {
            MaterialTimePickerHelper.formatEndTimeTo12h(it)
        } ?: selectText

        binding.tvEndTime.setTextColor(
            requireContext().getColor(
                if (endTime != null) R.color.trp_text_primary
                else R.color.trp_fgWeak
            )
        )

        binding.btnConfirm.isEnabled =
            startTime != null &&
            endTime != null &&
            MaterialTimePickerHelper.isEndTimeAfterStartTime(startTime, endTime)
    }

    private fun setupListeners() {
        binding.ivClose.setOnClickListener { dismiss() }

        binding.llStartTime.setOnClickListener {
            showStartTimePicker()
        }

        binding.llEndTime.setOnClickListener {
            showEndTimePicker()
        }

        binding.btnConfirm.setOnClickListener {
            onTimeSelectedListener?.invoke(startTime, endTime)
        }
    }

    /**
     * Opens the start-time picker floored at [minTime] (exclusive) so a past
     * start can't be chosen; defaults to the earliest selectable slot.
     */
    private fun showStartTimePicker() {
        showComposeTimePicker(
            initialTime = startTime ?: defaultStartTime,
            minTime = minTime,
            onTimeSelected = { hour, minute ->
                val time24h = MaterialTimePickerHelper.formatTo24h(hour, minute)
                startTime = time24h

                if (endTime != null && !MaterialTimePickerHelper.isEndTimeAfterStartTime(time24h, endTime)) {
                    endTime = null
                }

                updateTimeDisplays()
            }
        )
    }

    /**
     * Opens the end-time picker (min = later of start and [minTime]). A stored
     * "23:59" end is the midnight sentinel, so the clock seeds at 00:00.
     */
    private fun showEndTimePicker() {
        val initialTime = when {
            endTime == MaterialTimePickerHelper.END_OF_DAY_24H -> "00:00"
            endTime != null -> endTime
            else -> MaterialTimePickerHelper.addMinutes(startTime, 60) ?: startTime
        }
        showComposeTimePicker(
            initialTime = initialTime,
            minTime = MaterialTimePickerHelper.laterOf(startTime, minTime),
            treatMidnightAsEndOfDay = true,
            onTimeSelected = { hour, minute ->
                val time24h = MaterialTimePickerHelper.formatEndTimeTo24h(hour, minute)
                endTime = time24h
                updateTimeDisplays()
            }
        )
    }

    /**
     * [listener] owns the sheet's lifecycle from here: confirming no longer
     * auto-dismisses, so the caller can show [showInSheetLoadingOverlay] while its
     * (usually async) operation runs and dismiss the sheet itself once it succeeds.
     */
    fun setOnTimeSelectedListener(listener: (startTime: String?, endTime: String?) -> Unit) {
        onTimeSelectedListener = listener
    }

    companion object {
        const val TAG = "TimeSelectionBottomSheet"

        private const val ARG_START_TIME = "start_time"
        private const val ARG_END_TIME = "end_time"
        private const val ARG_MIN_TIME = "min_time"
        private const val ARG_DEFAULT_START_TIME = "default_start_time"

        fun newInstance(
            startTime: String? = null,
            endTime: String? = null,
            // Earliest selectable "HH:mm" (city-timezone "now" for the item's day).
            minTime: String? = null,
            // Suggested "HH:mm" prefill for the start-time picker when startTime is null.
            defaultStartTime: String? = null
        ): TimeSelectionBottomSheet {
            return TimeSelectionBottomSheet().apply {
                arguments = Bundle().apply {
                    startTime?.let { putString(ARG_START_TIME, it) }
                    endTime?.let { putString(ARG_END_TIME, it) }
                    minTime?.let { putString(ARG_MIN_TIME, it) }
                    defaultStartTime?.let { putString(ARG_DEFAULT_START_TIME, it) }
                }
            }
        }
    }
}
