package com.tripian.trpcore.ui.timeline.addplan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tripian.trpcore.R
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.BottomSheetTimePickerBinding
import com.tripian.trpcore.util.LanguageConst

/**
 * TimePickerBottomSheet
 * iOS-style time picker bottom sheet for start and end time selection
 */
class TimePickerBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetTimePickerBinding? = null
    private val binding get() = _binding!!

    private var onTimeSelectedListener: ((startTime: String?, endTime: String?) -> Unit)? = null

    // Current selection mode: true = start time, false = end time
    private var isSelectingStartTime = true

    // Selected times (HH:mm format in 24h)
    private var selectedStartTime: String? = null
    private var selectedEndTime: String? = null

    // Flag to track if endTime was initially set (not empty) when bottom sheet opened
    // If true, auto-calculation of endTime will be disabled
    private var wasEndTimeInitiallySet: Boolean = false

    // Duration in minutes for auto-calculating end time (default: 60 minutes)
    private var durationMinutes: Int = DEFAULT_DURATION_MINUTES

    // Flag to completely disable auto end time calculation (for smart recommendations)
    private var disableAutoEndTime: Boolean = false

    // AM/PM values
    private val amPmValues = arrayOf("AM", "PM")

    override fun getTheme(): Int = R.style.TimelineBottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetTimePickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Restore initial values from arguments
        arguments?.let { args ->
            selectedStartTime = args.getString(ARG_START_TIME)
            selectedEndTime = args.getString(ARG_END_TIME)
            durationMinutes = args.getInt(ARG_DURATION, DEFAULT_DURATION_MINUTES)
            disableAutoEndTime = args.getBoolean(ARG_DISABLE_AUTO_END_TIME, false)

            // Track if endTime was initially set - if so, disable auto-calculation
            wasEndTimeInitiallySet = !selectedEndTime.isNullOrEmpty()
        }

        setupLabels()
        setupNumberPickers()
        setupTabSelection()
        setupListeners()
        updateUI()
    }

    /**
     * Set all label texts using language service
     */
    private fun setupLabels() {
        val getLanguage: (String) -> String = { key ->
            TRPCore.core.miscRepository.getLanguageValueForKey(key)
        }

        binding.tvTitle.text = getLanguage(LanguageConst.ADD_PLAN_TIME)
        binding.tvStartTimeLabel.text = getLanguage(LanguageConst.ADD_PLAN_START_TIME)
        binding.tvEndTimeLabel.text = getLanguage(LanguageConst.ADD_PLAN_END_TIME)
        binding.btnConfirm.text = getLanguage(LanguageConst.ADD_PLAN_CONFIRM)
    }

    private fun setupNumberPickers() {
        // Hour picker (1-12 for 12h format)
        binding.npHour.minValue = 1
        binding.npHour.maxValue = 12
        binding.npHour.wrapSelectorWheel = true

        // Minute picker (00-59)
        binding.npMinute.minValue = 0
        binding.npMinute.maxValue = 59
        binding.npMinute.wrapSelectorWheel = true
        binding.npMinute.setFormatter { String.format("%02d", it) }

        // AM/PM picker
        binding.npAmPm.minValue = 0
        binding.npAmPm.maxValue = 1
        binding.npAmPm.displayedValues = amPmValues
        binding.npAmPm.wrapSelectorWheel = true

        // Set default time (8:00 AM)
        binding.npHour.value = 8
        binding.npMinute.value = 0
        binding.npAmPm.value = 0 // AM

        // Listen for changes
        val valueChangeListener = NumberPicker.OnValueChangeListener { _, _, _ ->
            updateCurrentTimeFromPicker()
        }
        binding.npHour.setOnValueChangedListener(valueChangeListener)
        binding.npMinute.setOnValueChangedListener(valueChangeListener)
        binding.npAmPm.setOnValueChangedListener(valueChangeListener)
    }

    private fun setupTabSelection() {
        binding.btnStartTimeTab.setOnClickListener {
            isSelectingStartTime = true
            updateTabSelection()
            loadTimeIntoPicker(selectedStartTime)
        }

        binding.btnEndTimeTab.setOnClickListener {
            isSelectingStartTime = false
            updateTabSelection()
            loadTimeIntoPicker(selectedEndTime)
        }

        // Default: start time tab selected
        updateTabSelection()
    }

    private fun setupListeners() {
        binding.ivClose.setOnClickListener {
            dismiss()
        }

        binding.btnConfirm.setOnClickListener {
            // Save current picker value before confirming
            updateCurrentTimeFromPicker()
            onTimeSelectedListener?.invoke(selectedStartTime, selectedEndTime)
            dismiss()
        }
    }

    private fun updateTabSelection() {
        binding.btnStartTimeTab.isSelected = isSelectingStartTime
        binding.btnEndTimeTab.isSelected = !isSelectingStartTime
    }

    private fun updateUI() {
        // Update display texts
        updateDisplayTexts()

        // Update confirm button state
        updateConfirmButtonState()

        // Load current time into picker based on selection mode
        if (isSelectingStartTime) {
            loadTimeIntoPicker(selectedStartTime)
        } else {
            loadTimeIntoPicker(selectedEndTime)
        }
    }

    /**
     * Update confirm button enabled state
     * Both times must be selected and end time must be after start time
     */
    private fun updateConfirmButtonState() {
        val isValid = selectedStartTime != null &&
                      selectedEndTime != null &&
                      isEndTimeAfterStartTime()

        binding.btnConfirm.isEnabled = isValid
    }

    /**
     * Check if end time is after start time
     */
    private fun isEndTimeAfterStartTime(): Boolean {
        val startMinutes = timeToMinutes(selectedStartTime) ?: return false
        val endMinutes = timeToMinutes(selectedEndTime) ?: return false
        return endMinutes > startMinutes
    }

    /**
     * Convert time string (HH:mm) to total minutes for comparison
     */
    private fun timeToMinutes(time: String?): Int? {
        if (time == null) return null
        val parts = time.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        return hour * 60 + minute
    }

    private fun updateDisplayTexts() {
        val selectText = TRPCore.core.miscRepository.getLanguageValueForKey(LanguageConst.ADD_PLAN_SELECT_TIME)

        binding.tvStartTimeDisplay.text = selectedStartTime?.let { convert24hTo12h(it) } ?: selectText
        binding.tvEndTimeDisplay.text = selectedEndTime?.let { convert24hTo12h(it) } ?: selectText

        // Check if end time is invalid (before start time)
        val isEndTimeInvalid = selectedStartTime != null &&
                               selectedEndTime != null &&
                               !isEndTimeAfterStartTime()

        // Update text color based on selection and validation
        binding.tvStartTimeDisplay.setTextColor(
            requireContext().getColor(
                if (selectedStartTime != null) R.color.text_primary else R.color.fgWeak
            )
        )
        binding.tvEndTimeDisplay.setTextColor(
            requireContext().getColor(
                when {
                    isEndTimeInvalid -> R.color.error_message  // Show red if end time is before start time
                    selectedEndTime != null -> R.color.text_primary
                    else -> R.color.fgWeak
                }
            )
        )
    }

    private fun loadTimeIntoPicker(time: String?) {
        if (time != null) {
            // Parse 24h format (HH:mm)
            val parts = time.split(":")
            if (parts.size == 2) {
                val hour24 = parts[0].toIntOrNull() ?: 8
                val minute = parts[1].toIntOrNull() ?: 0

                // Convert to 12h format
                val isPM = hour24 >= 12
                val hour12 = when {
                    hour24 == 0 -> 12
                    hour24 > 12 -> hour24 - 12
                    else -> hour24
                }

                binding.npHour.value = hour12
                binding.npMinute.value = minute
                binding.npAmPm.value = if (isPM) 1 else 0
            }
        } else {
            // Set default values for new selection
            if (isSelectingStartTime) {
                binding.npHour.value = 10
                binding.npMinute.value = 0
                binding.npAmPm.value = 0 // AM
            } else {
                binding.npHour.value = 6
                binding.npMinute.value = 0
                binding.npAmPm.value = 1 // PM
            }
        }
    }

    private fun updateCurrentTimeFromPicker() {
        val hour12 = binding.npHour.value
        val minute = binding.npMinute.value
        val isPM = binding.npAmPm.value == 1

        // Convert to 24h format
        val hour24 = when {
            hour12 == 12 && !isPM -> 0      // 12 AM = 00
            hour12 == 12 && isPM -> 12      // 12 PM = 12
            isPM -> hour12 + 12             // PM: add 12
            else -> hour12                  // AM: as is
        }

        val timeString = String.format("%02d:%02d", hour24, minute)

        if (isSelectingStartTime) {
            selectedStartTime = timeString
            // Auto-calculate end time ONLY if:
            // 1. disableAutoEndTime is false (not smart recommendations)
            // 2. endTime was not initially set
            if (!disableAutoEndTime && !wasEndTimeInitiallySet) {
                autoCalculateEndTime(hour24, minute)
            }
        } else {
            selectedEndTime = timeString
        }

        updateDisplayTexts()
        updateConfirmButtonState()
    }

    /**
     * Auto-calculate end time based on start time and duration
     * Uses POI duration if available, otherwise uses default 60 minutes
     */
    private fun autoCalculateEndTime(startHour24: Int, startMinute: Int) {
        val totalStartMinutes = startHour24 * 60 + startMinute
        val totalEndMinutes = totalStartMinutes + durationMinutes

        // Handle day overflow (cap at 23:59)
        val cappedEndMinutes = totalEndMinutes.coerceAtMost(23 * 60 + 59)

        val endHour24 = cappedEndMinutes / 60
        val endMinute = cappedEndMinutes % 60

        selectedEndTime = String.format("%02d:%02d", endHour24, endMinute)
    }

    /**
     * Convert 24h format (HH:mm) to 12h display format (h:mm AM/PM)
     */
    private fun convert24hTo12h(time24: String): String {
        val parts = time24.split(":")
        if (parts.size != 2) return time24

        val hour24 = parts[0].toIntOrNull() ?: return time24
        val minute = parts[1].toIntOrNull() ?: return time24

        val isPM = hour24 >= 12
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }

        val amPm = if (isPM) "PM" else "AM"
        return String.format("%d:%02d %s", hour12, minute, amPm)
    }

    fun setOnTimeSelectedListener(listener: (startTime: String?, endTime: String?) -> Unit) {
        onTimeSelectedListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "TimePickerBottomSheet"
        private const val ARG_START_TIME = "startTime"
        private const val ARG_END_TIME = "endTime"
        private const val ARG_DURATION = "duration"
        private const val ARG_DISABLE_AUTO_END_TIME = "disableAutoEndTime"
        private const val DEFAULT_DURATION_MINUTES = 60

        /**
         * Create a new instance of TimePickerBottomSheet
         * @param startTime Initial start time in HH:mm format (optional)
         * @param endTime Initial end time in HH:mm format (optional)
         * @param durationMinutes Duration in minutes for auto-calculating end time.
         *                        If null or 0, uses default 60 minutes.
         * @param disableAutoEndTime If true, auto end time calculation is completely disabled.
         *                           Used for smart recommendations where user selects both times manually.
         */
        fun newInstance(
            startTime: String? = null,
            endTime: String? = null,
            durationMinutes: Int? = null,
            disableAutoEndTime: Boolean = false
        ): TimePickerBottomSheet {
            return TimePickerBottomSheet().apply {
                arguments = Bundle().apply {
                    startTime?.let { putString(ARG_START_TIME, it) }
                    endTime?.let { putString(ARG_END_TIME, it) }
                    putInt(ARG_DURATION, durationMinutes?.takeIf { it > 0 } ?: DEFAULT_DURATION_MINUTES)
                    putBoolean(ARG_DISABLE_AUTO_END_TIME, disableAutoEndTime)
                }
            }
        }
    }
}
