package com.tripian.trpcore.ui.timeline.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.tripian.one.api.tour.model.TourProduct
import com.tripian.one.api.tour.model.TourScheduleSlot
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseBottomDialogFragment
import com.tripian.trpcore.databinding.BottomSheetActivityTimeSelectionBinding
import com.tripian.trpcore.ui.timeline.adapter.DayFilterAdapter
import com.tripian.trpcore.util.LanguageConst
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ActivityTimeSelectionBottomSheet
 * Bottom sheet for selecting a time slot for an activity/tour
 * iOS Reference: ActivityTimeSelectionView
 *
 * Uses its own ViewModel (ActivityTimeSelectionVM) to handle schedule loading.
 */
class ActivityTimeSelectionBottomSheet : BaseBottomDialogFragment<BottomSheetActivityTimeSelectionBinding, ActivityTimeSelectionVM>(
    BottomSheetActivityTimeSelectionBinding::inflate
) {

    private var dayAdapter: DayFilterAdapter? = null

    private var activity: TourProduct? = null
    private var availableDays: List<Date> = emptyList()
    private var selectedDayIndex: Int = 0
    private var selectedTimeSlot: String? = null
    private var currentSlots: List<TourScheduleSlot> = emptyList()

    private var onTimeSelectedListener: ((TourProduct, Date, String) -> Unit)? = null

    // For favorites mode (uses activityId for schedule API)
    private var isFavoriteMode: Boolean = false
    private var favoriteActivityId: String? = null
    private var favoriteCityId: Int? = null
    private var favoriteTitle: String? = null
    private var favoriteDuration: Double? = null
    private var onFavoriteTimeSelectedListener: ((Date, String?, String?) -> Unit)? = null

    override fun getTheme(): Int = R.style.TimelineBottomSheetDialog

    override fun setListeners() {
        super.setListeners()

        // Restore from arguments
        @Suppress("DEPRECATION")
        arguments?.let { args ->
            activity = args.getSerializable(ARG_ACTIVITY) as? TourProduct
            availableDays = (args.getSerializable(ARG_AVAILABLE_DAYS) as? ArrayList<Date>) ?: emptyList()
            isFavoriteMode = args.getBoolean(ARG_FAVORITE_MODE, false)
            favoriteActivityId = args.getString(ARG_FAVORITE_ACTIVITY_ID)
            favoriteCityId = args.getInt(ARG_FAVORITE_CITY_ID, 0).takeIf { it > 0 }
            favoriteTitle = args.getString(ARG_FAVORITE_TITLE)
            favoriteDuration = args.getDouble(ARG_FAVORITE_DURATION, 0.0).takeIf { it > 0 }

            val initialDay = args.getSerializable(ARG_INITIAL_SELECTED_DAY) as? Date
            selectedDayIndex = if (initialDay != null) {
                availableDays.indexOfFirst { isSameDay(it, initialDay) }.coerceAtLeast(0)
            } else {
                0
            }
        }

        setupUI()
        setupDayFilter()
        setupClickListeners()

        // Request schedule from API (both for tours and favorites)
        requestScheduleLoad()
    }

    override fun setReceivers() {
        super.setReceivers()

        // Observe schedule slots from ViewModel
        viewModel.scheduleSlots.observe(viewLifecycleOwner) { slots ->
            updateSchedule(slots)
        }

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            setLoading(isLoading)
        }
    }

    private fun setupUI() {
        // Set localized texts
        updateTexts()
        // Update continue button state
        updateContinueButtonState()
    }

    private fun updateTexts() {
        binding.tvTitle.text = getLanguageForKey(LanguageConst.ADD_PLAN_TITLE)
        binding.tvSelectTime.text = getLanguageForKey(LanguageConst.ADD_PLAN_SELECT_TIME)
        binding.tvNoTimeSlots.text = getLanguageForKey(LanguageConst.ADD_PLAN_NO_TIME_SLOTS)
        binding.btnContinue.text = getLanguageForKey(LanguageConst.ADD_PLAN_CONTINUE)
    }

    private fun setupDayFilter() {
        dayAdapter = DayFilterAdapter { index ->
            selectedDayIndex = index
            selectedTimeSlot = null
            dayAdapter?.setSelectedPosition(index)
            clearTimeSlotSelection()
            updateContinueButtonState()
            requestScheduleLoad()
        }
        binding.rvDays.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = dayAdapter
        }
        dayAdapter?.setDays(availableDays)
        dayAdapter?.setSelectedPosition(selectedDayIndex)
    }

    private fun setupClickListeners() {
        // Back button
        binding.ivBack.setOnClickListener {
            dismiss()
        }

        // Continue button - directly add activity without confirmation
        binding.btnContinue.setOnClickListener {
            addActivity()
        }
    }

    private fun addActivity() {
        val date = availableDays.getOrNull(selectedDayIndex) ?: return
        val timeSlot = selectedTimeSlot ?: return

        if (isFavoriteMode) {
            // For favorites - calculate end time from duration
            val endTime = calculateEndTimeFromDuration(timeSlot, favoriteDuration)
            onFavoriteTimeSelectedListener?.invoke(date, timeSlot, endTime)
        } else {
            // For tours
            val tour = activity ?: return
            onTimeSelectedListener?.invoke(tour, date, timeSlot)
        }
    }

    /**
     * Calculate end time from start time and duration
     */
    private fun calculateEndTimeFromDuration(startTime: String, duration: Double?): String? {
        if (duration == null || duration <= 0) return null

        try {
            val parts = startTime.split(":")
            val startHour = parts[0].toInt()
            val startMinute = parts[1].toInt()

            val totalMinutes = startHour * 60 + startMinute + duration.toInt()
            val endHour = (totalMinutes / 60) % 24
            val endMinute = totalMinutes % 60

            return String.format("%02d:%02d", endHour, endMinute)
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Request schedule loading via ViewModel
     * Works for both tour mode (using activity.id) and favorites mode (using favoriteActivityId)
     */
    private fun requestScheduleLoad() {
        // Get activity ID from either tour or favorite
        val activityId = if (isFavoriteMode) {
            favoriteActivityId
        } else {
            activity?.id
        }

        if (activityId == null) return
        val date = availableDays.getOrNull(selectedDayIndex) ?: return

        // Request schedule via ViewModel
        // For favorites, pass cityId for proper activityId formatting
        val cityId = if (isFavoriteMode) favoriteCityId else null
        viewModel.loadSchedule(activityId, date, cityId)
    }

    /**
     * Update the loading state UI
     */
    private fun setLoading(isLoading: Boolean) {
        binding.pbLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) {
            binding.tvNoTimeSlots.visibility = View.GONE
            binding.flexTimeSlots.visibility = View.GONE
        }
    }

    /**
     * Update the schedule slots UI
     */
    private fun updateSchedule(slots: List<TourScheduleSlot>?) {
        binding.pbLoading.visibility = View.GONE
        currentSlots = slots ?: emptyList()

        if (currentSlots.isEmpty()) {
            binding.tvNoTimeSlots.visibility = View.VISIBLE
            binding.flexTimeSlots.visibility = View.GONE
        } else {
            binding.tvNoTimeSlots.visibility = View.GONE
            binding.flexTimeSlots.visibility = View.VISIBLE
            populateTimeSlots(currentSlots)
        }
    }

    private fun populateTimeSlots(slots: List<TourScheduleSlot>) {
        binding.flexTimeSlots.removeAllViews()

        val inflater = LayoutInflater.from(requireContext())
        val density = resources.displayMetrics.density
        val heightPx = (36 * density).toInt()
        val marginPx = (8 * density).toInt()

        // Calculate item width for exactly 4 columns
        // FlexboxLayout has 24sdp margin on each side (48sdp total)
        val flexboxMarginPx = (24 * density * 2).toInt()
        val screenWidth = resources.displayMetrics.widthPixels
        val availableWidth = screenWidth - flexboxMarginPx
        // 4 items with 4 right margins (last item margin will overflow but FlexboxLayout handles wrap)
        val columnCount = 4
        val totalMargins = columnCount * marginPx
        val itemWidthPx = (availableWidth - totalMargins) / columnCount

        slots.forEach { slot ->
            val chipView = inflater.inflate(R.layout.item_time_slot, binding.flexTimeSlots, false) as TextView
            chipView.text = slot.time ?: ""

            val isSelected = slot.time == selectedTimeSlot
            chipView.isSelected = isSelected
            chipView.isActivated = isSelected

            chipView.setOnClickListener {
                selectedTimeSlot = slot.time
                updateTimeSlotSelection()
                updateContinueButtonState()
            }

            // Set layout params for FlexboxLayout with fixed width for 4 columns
            val params = com.google.android.flexbox.FlexboxLayout.LayoutParams(
                itemWidthPx,
                heightPx
            )
            params.setMargins(0, 0, marginPx, marginPx)
            chipView.layoutParams = params

            binding.flexTimeSlots.addView(chipView)
        }
    }

    private fun updateTimeSlotSelection() {
        for (i in 0 until binding.flexTimeSlots.childCount) {
            val child = binding.flexTimeSlots.getChildAt(i) as? TextView
            val slot = currentSlots.getOrNull(i)
            val isSelected = slot?.time == selectedTimeSlot
            child?.isSelected = isSelected
            child?.isActivated = isSelected
        }
    }

    private fun clearTimeSlotSelection() {
        for (i in 0 until binding.flexTimeSlots.childCount) {
            val child = binding.flexTimeSlots.getChildAt(i) as? TextView
            child?.isSelected = false
            child?.isActivated = false
        }
    }

    private fun updateContinueButtonState() {
        val isValid = selectedTimeSlot != null
        binding.btnContinue.isEnabled = isValid
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return format.format(date1) == format.format(date2)
    }

    fun setOnTimeSelectedListener(listener: (TourProduct, Date, String) -> Unit) {
        onTimeSelectedListener = listener
    }

    /**
     * Set listener for favorite time selection (used in favorites mode)
     * @param listener Callback with (selectedDate, startTime, endTime)
     */
    fun setOnFavoriteTimeSelectedListener(listener: (Date, String?, String?) -> Unit) {
        onFavoriteTimeSelectedListener = listener
    }

    companion object {
        const val TAG = "ActivityTimeSelectionBottomSheet"
        private const val ARG_ACTIVITY = "activity"
        private const val ARG_AVAILABLE_DAYS = "available_days"
        private const val ARG_INITIAL_SELECTED_DAY = "initial_selected_day"
        private const val ARG_FAVORITE_MODE = "favorite_mode"
        private const val ARG_FAVORITE_ACTIVITY_ID = "favorite_activity_id"
        private const val ARG_FAVORITE_CITY_ID = "favorite_city_id"
        private const val ARG_FAVORITE_TITLE = "favorite_title"
        private const val ARG_FAVORITE_DURATION = "favorite_duration"

        /**
         * Create instance for TourProduct (with API schedule loading)
         */
        fun newInstance(
            activity: TourProduct,
            availableDays: List<Date>,
            initialSelectedDay: Date? = null
        ): ActivityTimeSelectionBottomSheet {
            return ActivityTimeSelectionBottomSheet().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_ACTIVITY, activity)
                    putSerializable(ARG_AVAILABLE_DAYS, ArrayList(availableDays))
                    initialSelectedDay?.let { putSerializable(ARG_INITIAL_SELECTED_DAY, it) }
                }
            }
        }

        /**
         * Create instance for SegmentFavoriteItem (with API schedule loading using activityId)
         */
        fun newInstanceForFavorite(
            favoriteActivityId: String?,
            favoriteCityId: Int?,
            favoriteTitle: String,
            favoriteDuration: Double?,
            availableDays: List<Date>,
            initialSelectedDay: Date? = null
        ): ActivityTimeSelectionBottomSheet {
            return ActivityTimeSelectionBottomSheet().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_FAVORITE_MODE, true)
                    favoriteActivityId?.let { putString(ARG_FAVORITE_ACTIVITY_ID, it) }
                    favoriteCityId?.let { putInt(ARG_FAVORITE_CITY_ID, it) }
                    putString(ARG_FAVORITE_TITLE, favoriteTitle)
                    favoriteDuration?.let { putDouble(ARG_FAVORITE_DURATION, it) }
                    putSerializable(ARG_AVAILABLE_DAYS, ArrayList(availableDays))
                    initialSelectedDay?.let { putSerializable(ARG_INITIAL_SELECTED_DAY, it) }
                }
            }
        }
    }
}
