package com.tripian.trpcore.ui.timeline.activity

import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.tripian.one.api.tour.model.TourProduct
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseBottomDialogFragment
import com.tripian.trpcore.base.TRPCore
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
    private var selectedPrice: Double? = null
    private var currentSlots: List<GroupedTimeSlot> = emptyList()
    // Mirrors ResolvedSchedule so click handlers know whether the user is
    // committing to a timed slot or the flexible / "Any time" path.
    private var currentMode: TimeSelectionMode = TimeSelectionMode.TIMED
    private var currentFlexiblePrice: Double? = null
    private var isFlexibleSelected: Boolean = false

    private var onTimeSelectedListener: ((TourProduct, Date, String, Double?, Boolean) -> Unit)? = null

    // For favorites mode (uses activityId for schedule API)
    private var isFavoriteMode: Boolean = false
    private var favoriteActivityId: String? = null
    private var favoriteCityId: Int? = null
    private var favoriteTitle: String? = null
    private var favoriteDuration: Double? = null
    private var onFavoriteTimeSelectedListener: ((Date, String?, String?) -> Unit)? = null

    override fun getTheme(): Int = R.style.TrpTimelineBottomSheetDialog

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

        // Observe resolved schedule (mode + timed slots + flexible price)
        // from ViewModel. Loading is surfaced as a bottom-sheet Lottie via
        // BaseBottomDialogFragment's central observer.
        viewModel.resolvedSchedule.observe(viewLifecycleOwner) { resolved ->
            updateSchedule(resolved)
        }

        // Disable day-filter cells for days that have no slots, and bump the
        // selection forward if the initially-selected day turned out empty.
        viewModel.availableDateStrings.observe(viewLifecycleOwner) { available ->
            dayAdapter?.availableDateStrings = available
            jumpToFirstAvailableIfNeeded(available)
        }
    }

    private val dayKeyFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun jumpToFirstAvailableIfNeeded(available: Set<String>?) {
        if (available.isNullOrEmpty()) return
        val current = availableDays.getOrNull(selectedDayIndex) ?: return
        if (dayKeyFormatter.format(current) in available) return

        val firstAvailableIndex = availableDays.indexOfFirst {
            dayKeyFormatter.format(it) in available
        }
        if (firstAvailableIndex < 0 || firstAvailableIndex == selectedDayIndex) return

        selectedDayIndex = firstAvailableIndex
        selectedTimeSlot = null
        selectedPrice = null
        isFlexibleSelected = false
        dayAdapter?.setSelectedPosition(firstAvailableIndex)
        clearTimeSlotSelection()
        updateContinueButtonState()
        availableDays.getOrNull(firstAvailableIndex)?.let { viewModel.selectDate(it) }
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
            selectedPrice = null
            isFlexibleSelected = false
            dayAdapter?.setSelectedPosition(index)
            clearTimeSlotSelection()
            updateContinueButtonState()
            // Schedule for the full trip range was already fetched in setListeners();
            // day switch just re-filters the cached response.
            availableDays.getOrNull(index)?.let { viewModel.selectDate(it) }
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

        if (isFlexibleSelected) {
            // Flexible activity — segment is created with 00:00/23:59 + duration -1
            // downstream. We forward "00:00" as a placeholder so the existing
            // string-typed callback contract is preserved; the isFlexible flag
            // is the source of truth.
            if (isFavoriteMode) {
                onFavoriteTimeSelectedListener?.invoke(date, "00:00", "23:59")
            } else {
                val tour = activity ?: return
                onTimeSelectedListener?.invoke(tour, date, "00:00", currentFlexiblePrice, true)
            }
            return
        }

        val timeSlot = selectedTimeSlot ?: return

        if (isFavoriteMode) {
            // For favorites - calculate end time from duration
            val endTime = calculateEndTimeFromDuration(timeSlot, favoriteDuration)
            onFavoriteTimeSelectedListener?.invoke(date, timeSlot, endTime)
        } else {
            // For tours - pass selected price (minimum price for the selected time slot)
            val tour = activity ?: return
            onTimeSelectedListener?.invoke(tour, date, timeSlot, selectedPrice, false)
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
     * Request schedule loading via ViewModel.
     * Called once on open — sends the full trip date range so the VM can cache
     * the response and serve day switches client-side via [ActivityTimeSelectionVM.selectDate].
     */
    private fun requestScheduleLoad() {
        // Get activity ID from either tour or favorite
        val activityId = if (isFavoriteMode) {
            favoriteActivityId
        } else {
            activity?.id
        }

        if (activityId == null) return
        if (availableDays.isEmpty()) return
        val selectedDate = availableDays.getOrNull(selectedDayIndex) ?: availableDays.first()

        // For favorites, pass cityId for proper activityId formatting
        val cityId = if (isFavoriteMode) favoriteCityId else null
        viewModel.loadSchedule(activityId, availableDays, selectedDate, cityId)
    }

    /**
     * Render whichever combination of (time grid / flexible info card /
     * empty-state label) is appropriate for the resolved schedule. See
     * [TimeSelectionMode] for the three shapes.
     */
    private fun updateSchedule(resolved: ResolvedSchedule?) {
        val safe = resolved ?: ResolvedSchedule(
            mode = TimeSelectionMode.TIMED,
            timedSlots = emptyList(),
            flexiblePrice = null
        )

        currentMode = safe.mode
        currentFlexiblePrice = safe.flexiblePrice
        currentSlots = safe.timedSlots
        // Switching days/modes resets any previous selection.
        selectedTimeSlot = null
        selectedPrice = null
        isFlexibleSelected = false

        when (safe.mode) {
            TimeSelectionMode.FLEXIBLE_ONLY -> {
                binding.tvSelectTime.visibility = View.GONE
                binding.flexTimeSlots.visibility = View.GONE
                binding.tvNoTimeSlots.visibility = View.GONE
                binding.flexibleInfoCard.visibility = View.VISIBLE
                binding.tvFlexibleTopOfItinerary.visibility = View.VISIBLE
                applyFlexibleInfoTexts()
                // Continue is enabled immediately — no chip selection needed.
                isFlexibleSelected = true
            }
            TimeSelectionMode.TIMED, TimeSelectionMode.MIXED -> {
                binding.tvSelectTime.visibility = View.VISIBLE
                binding.flexibleInfoCard.visibility = View.GONE
                binding.tvFlexibleTopOfItinerary.visibility = View.GONE

                if (currentSlots.isEmpty() && safe.mode == TimeSelectionMode.TIMED) {
                    binding.tvNoTimeSlots.visibility = View.VISIBLE
                    binding.flexTimeSlots.visibility = View.GONE
                } else {
                    binding.tvNoTimeSlots.visibility = View.GONE
                    binding.flexTimeSlots.visibility = View.VISIBLE
                    // Theme 9: render the collapsed window (first N slots) when the VM
                    // reports we should be in the show-more state.
                    populateTimeSlots(viewModel.getDisplayedSlots())
                }
            }
        }

        updateContinueButtonState()
    }

    private fun applyFlexibleInfoTexts() {
        binding.tvFlexibleTitle.text = TRPCore.core.miscRepository
            .getLanguageValueForKey(LanguageConst.ADD_PLAN_FLEXIBLE_INFO_TITLE)
            .ifBlank { "Valid at any time on the chosen day. Check the opening hours." }
        binding.tvFlexibleTopOfItinerary.text = TRPCore.core.miscRepository
            .getLanguageValueForKey(LanguageConst.ADD_PLAN_FLEXIBLE_TOP_OF_ITINERARY)
            .ifBlank { "Add it to top of your itinerary." }
    }

    private fun populateTimeSlots(slots: List<GroupedTimeSlot>) {
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
            chipView.text = slot.time

            val isSelected = !isFlexibleSelected && slot.time == selectedTimeSlot
            chipView.isSelected = isSelected
            chipView.isActivated = isSelected

            chipView.setOnClickListener {
                selectedTimeSlot = slot.time
                selectedPrice = slot.minPrice
                isFlexibleSelected = false
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

        // MIXED day: append an extra "Any time" chip after the timed slots.
        // Tapping it selects the flexible slot (no specific time).
        if (currentMode == TimeSelectionMode.MIXED) {
            val anyTimeView = inflater.inflate(R.layout.item_time_slot, binding.flexTimeSlots, false) as TextView
            anyTimeView.text = TRPCore.core.miscRepository
                .getLanguageValueForKey(LanguageConst.ADD_PLAN_FLEXIBLE_ANY_TIME)
                .ifBlank { "Any time" }
            anyTimeView.isSelected = isFlexibleSelected
            anyTimeView.isActivated = isFlexibleSelected
            anyTimeView.setOnClickListener {
                isFlexibleSelected = true
                selectedTimeSlot = null
                selectedPrice = null
                updateTimeSlotSelection()
                updateContinueButtonState()
            }
            val params = com.google.android.flexbox.FlexboxLayout.LayoutParams(
                itemWidthPx,
                heightPx
            )
            params.setMargins(0, 0, marginPx, marginPx)
            anyTimeView.layoutParams = params
            binding.flexTimeSlots.addView(anyTimeView)
        }

        // Theme 9: append "Show more times" cell when the collapsed window is active.
        if (viewModel.shouldShowMoreCell) {
            val showMoreView = inflater.inflate(
                R.layout.item_time_slot_show_more,
                binding.flexTimeSlots,
                false
            ) as TextView
            showMoreView.text = TRPCore.core.miscRepository
                .getLanguageValueForKey(LanguageConst.ADD_PLAN_TS_SHOW_MORE)
                .ifBlank { "Show more times" }
            showMoreView.paintFlags = showMoreView.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            showMoreView.setOnClickListener {
                viewModel.expandTimeSlots()
                populateTimeSlots(viewModel.getDisplayedSlots())
            }
            val params = com.google.android.flexbox.FlexboxLayout.LayoutParams(
                itemWidthPx,
                heightPx
            )
            params.setMargins(0, 0, marginPx, marginPx)
            showMoreView.layoutParams = params
            binding.flexTimeSlots.addView(showMoreView)
        }
    }

    private fun updateTimeSlotSelection() {
        val anyTimeLabel = TRPCore.core.miscRepository
            .getLanguageValueForKey(LanguageConst.ADD_PLAN_FLEXIBLE_ANY_TIME)
            .ifBlank { "Any time" }
        for (i in 0 until binding.flexTimeSlots.childCount) {
            val child = binding.flexTimeSlots.getChildAt(i) as? TextView ?: continue
            val isAnyTimeChip = currentMode == TimeSelectionMode.MIXED &&
                child.text?.toString() == anyTimeLabel
            val isSelected = when {
                isAnyTimeChip -> isFlexibleSelected
                else -> !isFlexibleSelected && child.text?.toString() == selectedTimeSlot
            }
            child.isSelected = isSelected
            child.isActivated = isSelected
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
        val isValid = isFlexibleSelected || selectedTimeSlot != null
        binding.btnContinue.isEnabled = isValid
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return format.format(date1) == format.format(date2)
    }

    /**
     * Set listener for time selection
     * @param listener Callback with (tour, selectedDate, timeSlot, slotMinPrice, isFlexible).
     *                 When `isFlexible == true`, [timeSlot] is the placeholder
     *                 "00:00" and the caller must build the segment with
     *                 00:00 / 23:59 + duration -1.
     */
    fun setOnTimeSelectedListener(
        listener: (TourProduct, Date, String, Double?, Boolean) -> Unit
    ) {
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
