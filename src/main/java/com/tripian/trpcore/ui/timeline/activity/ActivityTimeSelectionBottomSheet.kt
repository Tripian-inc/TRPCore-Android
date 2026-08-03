package com.tripian.trpcore.ui.timeline.activity

import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.tripian.one.api.tour.model.TourProduct
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseBottomDialogFragment
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.BottomSheetActivityTimeSelectionBinding
import com.tripian.trpcore.ui.timeline.adapter.DayFilterAdapter
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.asIdsByDay
import com.tripian.trpcore.util.extensions.toSerializableIdsByDay
import com.tripian.trpcore.util.widget.BottomToast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
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
    private var currentFlexiblePrice: Double? = null
    private var isFlexibleSelected: Boolean = false

    private var onTimeSelectedListener: ((TourProduct, Date, String, Double?, Boolean) -> Unit)? = null

    /** SavedPlans flow: primary button becomes "Select" and an outlined "Remove" button is shown. */
    private var showSelectAndRemove: Boolean = false
    private var onRemoveListener: (() -> Unit)? = null

    /** Favorites mode: the schedule API is called with the favorite's activityId. */
    private var isFavoriteMode: Boolean = false
    private var favoriteActivityId: String? = null
    private var favoriteCityId: Int? = null
    private var favoriteTitle: String? = null
    private var favoriteDuration: Double? = null
    /** Last param is the selected slot's min price (null when the slot carries no price). */
    private var onFavoriteTimeSelectedListener: ((Date, String?, String?, Boolean, Double?) -> Unit)? = null

    /** Step-edit mode shares the favorite schedule load path but confirms via a
     *  callback returning HH:mm start/end and seeds the grid with the step's current slot. */
    private var isStepEditMode: Boolean = false
    private var isActivityNotAvailable: Boolean = false
    private var pendingInitialTimeSlot: String? = null

    /** Recommendations activity steps can't move day, so the day filter row is hidden entirely. */
    private var hideDaySelector: Boolean = false

    /** "yyyy-MM-dd" → activity ids that day already holds. Empty in edit flows. */
    private var plannedActivityIdsByDay: Map<String, List<String>> = emptyMap()
    /** Last param is the selected slot's min price (null when the slot carries no price). */
    private var onStepTimeSelectedListener: ((Date, String, String?, Double?) -> Unit)? = null

    /**
     * Original booked time (`HH:mm`), rendered as a disabled chip when the user is
     * on the step's original day and the schedule no longer offers it. Null in
     * non-step-edit modes.
     */
    private var initialTimeSlot: String? = null

    /** `yyyy-MM-dd` key of the step's original day; the disabled chip only surfaces on this date. */
    private var initialDayKey: String? = null
    private var lastSlideOffset: Float = 1f

    /**
     * Adds slack to swipe-to-dismiss: the base sheet runs with `skipCollapsed`,
     * so any downward release hides it. This snaps the sheet back to expanded
     * unless it was dragged past [DISMISS_SLIDE_THRESHOLD] of the way down,
     * so a small over-scroll no longer closes the sheet.
     */
    private val dragDismissGuard = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            lastSlideOffset = slideOffset
        }

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_SETTLING &&
                lastSlideOffset > DISMISS_SLIDE_THRESHOLD
            ) {
                BottomSheetBehavior.from(bottomSheet).state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val sheet = dialog?.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        ) ?: return
        val behavior = BottomSheetBehavior.from(sheet)
        behavior.removeBottomSheetCallback(dragDismissGuard)
        behavior.addBottomSheetCallback(dragDismissGuard)
    }

    override fun setListeners() {
        super.setListeners()

        @Suppress("DEPRECATION")
        arguments?.let { args ->
            activity = args.getSerializable(ARG_ACTIVITY) as? TourProduct
            availableDays = (args.getSerializable(ARG_AVAILABLE_DAYS) as? ArrayList<Date>) ?: emptyList()
            isFavoriteMode = args.getBoolean(ARG_FAVORITE_MODE, false)
            favoriteActivityId = args.getString(ARG_FAVORITE_ACTIVITY_ID)
            favoriteCityId = args.getInt(ARG_FAVORITE_CITY_ID, 0).takeIf { it > 0 }
            favoriteTitle = args.getString(ARG_FAVORITE_TITLE)
            favoriteDuration = args.getDouble(ARG_FAVORITE_DURATION, 0.0).takeIf { it > 0 }
            isStepEditMode = args.getBoolean(ARG_STEP_EDIT_MODE, false)
            isActivityNotAvailable = args.getBoolean(ARG_NOT_AVAILABLE, false)
            hideDaySelector = args.getBoolean(ARG_HIDE_DAY_SELECTOR, false)
            showSelectAndRemove = args.getBoolean(ARG_SHOW_SELECT_AND_REMOVE, false)
            plannedActivityIdsByDay = args.getSerializable(ARG_PLANNED_ACTIVITY_IDS).asIdsByDay()
            pendingInitialTimeSlot = args.getString(ARG_INITIAL_TIME_SLOT)
            initialTimeSlot = pendingInitialTimeSlot

            val initialDay = args.getSerializable(ARG_INITIAL_SELECTED_DAY) as? Date
            initialDayKey = initialDay?.let { dayKeyFormatter.format(it) }
            selectedDayIndex = if (initialDay != null) {
                availableDays.indexOfFirst { isSameDay(it, initialDay) }.coerceAtLeast(0)
            } else {
                0
            }
        }

        setupUI()
        setupDayFilter()
        setupClickListeners()

        requestScheduleLoad()
    }

    override fun setReceivers() {
        super.setReceivers()

        viewModel.resolvedSchedule.observe(viewLifecycleOwner) { resolved ->
            updateSchedule(resolved)
        }

        viewModel.availableDateStrings.observe(viewLifecycleOwner) { available ->
            dayAdapter?.availableDateStrings = available
            jumpToFirstAvailableIfNeeded(available)
        }

        viewModel.isUnavailableForTrip.observe(viewLifecycleOwner) { unavailable ->
            applyTripUnavailableState(unavailable)
        }
    }

    /**
     * Replaces the slot grid area with a warning card when the trip range has
     * zero availability; the day filter stays visible with every cell disabled.
     */
    private fun applyTripUnavailableState(unavailable: Boolean) {
        if (unavailable) {
            binding.tvTripUnavailable.text = getLanguageForKey(viewModel.unavailableBannerKey())
            binding.tripUnavailableCard.visibility = View.VISIBLE
            binding.tvSelectTime.visibility = View.GONE
            binding.scrollTimeSlots.visibility = View.GONE
            binding.tvNoTimeSlots.visibility = View.GONE
            binding.flexibleInfoCard.visibility = View.GONE
            binding.tvFlexibleTopOfItinerary.visibility = View.GONE
            selectedTimeSlot = null
            selectedPrice = null
            isFlexibleSelected = false
            updateContinueButtonState()
        } else {
            binding.tripUnavailableCard.visibility = View.GONE
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
        updateTexts()
        binding.btnRemove.visibility =
            if (showSelectAndRemove || isStepEditMode) View.VISIBLE else View.GONE
        if (hideDaySelector) {
            binding.tvAddToDay.visibility = View.GONE
            binding.rvDays.visibility = View.GONE
        }
        updateContinueButtonState()
    }

    private fun updateTexts() {
        binding.tvTitle.text = if (isStepEditMode) {
            getLanguageForKey(LanguageConst.CHANGE_TIME)
        } else {
            getLanguageForKey(LanguageConst.ADD_PLAN_TITLE)
        }
        binding.tvAddToDay.text = if (isStepEditMode && !isActivityNotAvailable) {
            getLanguageForKey(LanguageConst.ADD_PLAN_MOVE_DAY)
        } else {
            getLanguageForKey(LanguageConst.ADD_PLAN_ADD_TO_DAY)
        }
        binding.tvSelectTime.text = getLanguageForKey(LanguageConst.ADD_PLAN_SELECT_A_TIME)
        binding.tvNoTimeSlots.text = getLanguageForKey(LanguageConst.ADD_PLAN_NO_TIME_SLOTS)
        binding.tvTripUnavailable.text = getLanguageForKey(LanguageConst.ADD_PLAN_ACTIVITY_NOT_AVAILABLE_TRIP_DAYS)
        binding.btnContinue.text = if (showSelectAndRemove) {
            getLanguageForKey(LanguageConst.ADD_PLAN_SELECT)
        } else {
            getLanguageForKey(LanguageConst.ADD_PLAN_CONTINUE)
        }
        binding.btnRemove.text = getLanguageForKey(LanguageConst.REMOVE_BUTTON)
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
            availableDays.getOrNull(index)?.let { viewModel.selectDate(it) }
        }
        binding.rvDays.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = dayAdapter
        }
        dayAdapter?.timeZoneId = com.tripian.trpcore.util.CityTimeZones.timezoneFor(favoriteCityId)
        dayAdapter?.setDays(availableDays)
        dayAdapter?.setSelectedPosition(selectedDayIndex)
    }

    private fun setupClickListeners() {
        binding.ivBack.setOnClickListener {
            dismiss()
        }

        binding.btnContinue.setOnClickListener {
            addActivity()
        }

        binding.btnRemove.setOnClickListener {
            onRemoveListener?.invoke()
        }
    }

    private fun addActivity() {
        val date = availableDays.getOrNull(selectedDayIndex) ?: return

        if (isFlexibleSelected) {
            if (isFavoriteMode) {
                onFavoriteTimeSelectedListener?.invoke(date, "00:00", "23:59", true, currentFlexiblePrice)
            } else {
                val tour = activity ?: return
                onTimeSelectedListener?.invoke(tour, date, "00:00", currentFlexiblePrice, true)
            }
            return
        }

        val timeSlot = selectedTimeSlot ?: return

        if (isStepEditMode) {
            val endTime = calculateEndTimeFromDuration(timeSlot, favoriteDuration)
            onStepTimeSelectedListener?.invoke(date, timeSlot, endTime, selectedPrice)
        } else if (isFavoriteMode) {
            val endTime = calculateEndTimeFromDuration(timeSlot, favoriteDuration)
            onFavoriteTimeSelectedListener?.invoke(date, timeSlot, endTime, false, selectedPrice)
        } else {
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
        val activityId = if (isFavoriteMode || isStepEditMode) {
            favoriteActivityId
        } else {
            activity?.id
        }

        if (activityId == null) return
        if (availableDays.isEmpty()) return
        val selectedDate = availableDays.getOrNull(selectedDayIndex) ?: availableDays.first()

        val cityId = if (isFavoriteMode || isStepEditMode) favoriteCityId else null
        viewModel.applyPlannedActivities(plannedActivityIdsByDay, activity?.productId ?: activityId)
        viewModel.loadSchedule(activityId, availableDays, selectedDate, cityId)
    }

    /**
     * Render whichever combination of (time grid / flexible info card /
     * empty-state label) is appropriate for the resolved schedule. See
     * [TimeSelectionMode] for the three shapes. Skips rendering entirely while
     * trip-wide unavailability is active so the warning card isn't overridden.
     */
    private fun updateSchedule(resolved: ResolvedSchedule?) {
        if (viewModel.isUnavailableForTrip.value == true) return

        val safe = resolved ?: ResolvedSchedule(
            mode = TimeSelectionMode.TIMED,
            timedSlots = emptyList(),
            flexiblePrice = null
        )

        currentFlexiblePrice = safe.flexiblePrice
        currentSlots = safe.timedSlots
        selectedTimeSlot = null
        selectedPrice = null
        isFlexibleSelected = false

        pendingInitialTimeSlot?.let { initialSlot ->
            val match = currentSlots.firstOrNull { it.time == initialSlot }
            if (match != null) {
                selectedTimeSlot = match.time
                selectedPrice = match.minPrice
                if (viewModel.getDisplayedSlots().none { it.time == match.time }) {
                    viewModel.expandTimeSlots()
                }
            }
            pendingInitialTimeSlot = null
        }

        when (safe.mode) {
            TimeSelectionMode.FLEXIBLE_ONLY -> {
                binding.tvSelectTime.visibility = View.GONE
                binding.scrollTimeSlots.visibility = View.GONE
                binding.tvNoTimeSlots.visibility = View.GONE
                binding.flexibleInfoCard.visibility = View.VISIBLE
                binding.tvFlexibleTopOfItinerary.visibility = View.VISIBLE
                applyFlexibleInfoTexts()
                isFlexibleSelected = true
            }
            TimeSelectionMode.TIMED -> {
                binding.tvSelectTime.visibility = View.VISIBLE
                binding.flexibleInfoCard.visibility = View.GONE
                binding.tvFlexibleTopOfItinerary.visibility = View.GONE

                if (currentSlots.isEmpty()) {
                    binding.tvNoTimeSlots.visibility = View.VISIBLE
                    binding.scrollTimeSlots.visibility = View.GONE
                } else {
                    binding.tvNoTimeSlots.visibility = View.GONE
                    binding.scrollTimeSlots.visibility = View.VISIBLE
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

    /**
     * Renders the slot chips in a fixed 4-column grid. Past slots for the city's
     * timezone are disabled; in step-edit mode on the step's original day, the
     * originally-booked time that the backend no longer offers is appended as a
     * disabled chip. All chips render in chronological order.
     */
    private fun populateTimeSlots(slots: List<GroupedTimeSlot>) {
        binding.flexTimeSlots.removeAllViews()

        val inflater = LayoutInflater.from(requireContext())
        val density = resources.displayMetrics.density
        val heightPx = (36 * density).toInt()
        val marginPx = (8 * density).toInt()

        val flexboxMarginPx = (24 * density * 2).toInt()
        val screenWidth = resources.displayMetrics.widthPixels
        val availableWidth = screenWidth - flexboxMarginPx
        val columnCount = 4
        val totalMargins = columnCount * marginPx
        val itemWidthPx = (availableWidth - totalMargins) / columnCount

        val currentDayKey = availableDays.getOrNull(selectedDayIndex)?.let {
            dayKeyFormatter.format(it)
        }
        val disabledTime = initialTimeSlot
            ?.takeIf {
                isStepEditMode &&
                    currentDayKey != null &&
                    currentDayKey == initialDayKey &&
                    currentSlots.none { slot -> slot.time == it }
            }

        val selectedDay = availableDays.getOrNull(selectedDayIndex)

        data class ChipSpec(val time: String, val price: Double?, val isDisabled: Boolean)
        val chips = mutableListOf<ChipSpec>()
        chips += slots.map { slot ->
            val isPast = selectedDay != null &&
                com.tripian.trpcore.util.CityTimeZones.isTimeSlotInPast(selectedDay, slot.time, favoriteCityId)
            ChipSpec(slot.time, slot.minPrice, isDisabled = isPast)
        }
        disabledTime?.let { chips += ChipSpec(it, price = null, isDisabled = true) }
        chips.sortBy { it.time }

        val disabledTextColor = androidx.core.content.ContextCompat.getColor(
            requireContext(),
            R.color.trp_white
        )

        chips.forEach { spec ->
            val chipView = inflater.inflate(R.layout.item_time_slot, binding.flexTimeSlots, false) as TextView
            chipView.text = spec.time

            if (spec.isDisabled) {
                chipView.setBackgroundResource(R.drawable.trp_bg_time_slot_disabled)
                chipView.setTextColor(disabledTextColor)
                chipView.isClickable = false
                chipView.isSelected = false
                chipView.isActivated = false
            } else {
                val isSelected = !isFlexibleSelected && spec.time == selectedTimeSlot
                chipView.isSelected = isSelected
                chipView.isActivated = isSelected

                chipView.setOnClickListener {
                    selectedTimeSlot = spec.time
                    selectedPrice = spec.price
                    isFlexibleSelected = false
                    updateTimeSlotSelection()
                    updateContinueButtonState()
                }
            }

            val params = com.google.android.flexbox.FlexboxLayout.LayoutParams(
                itemWidthPx,
                heightPx
            )
            params.setMargins(0, 0, marginPx, marginPx)
            chipView.layoutParams = params

            binding.flexTimeSlots.addView(chipView)
        }

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

        adjustSlotScrollHeight()
    }

    /**
     * Keeps the slot grid content-sized while collapsed and caps it to
     * [MAX_SLOT_SCROLL_HEIGHT_RATIO] of the screen once expanded, so a long fully
     * expanded list scrolls inside the grid instead of stretching the sheet.
     */
    private fun adjustSlotScrollHeight() {
        val scroll = binding.scrollTimeSlots
        if (!viewModel.isTimeSlotsExpanded) {
            setScrollHeight(scroll, ViewGroup.LayoutParams.WRAP_CONTENT)
            return
        }
        val maxHeightPx = (resources.displayMetrics.heightPixels * MAX_SLOT_SCROLL_HEIGHT_RATIO).toInt()
        scroll.post {
            if (!isAdded) return@post
            val contentHeight = binding.flexTimeSlots.height
            val target = if (contentHeight > maxHeightPx) maxHeightPx else ViewGroup.LayoutParams.WRAP_CONTENT
            setScrollHeight(scroll, target)
        }
    }

    private fun setScrollHeight(scroll: View, height: Int) {
        val params = scroll.layoutParams
        if (params.height != height) {
            params.height = height
            scroll.layoutParams = params
        }
    }

    private fun updateTimeSlotSelection() {
        for (i in 0 until binding.flexTimeSlots.childCount) {
            val child = binding.flexTimeSlots.getChildAt(i) as? TextView ?: continue
            val isSelected = child.text?.toString() == selectedTimeSlot
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
     * @param listener Callback with (selectedDate, startTime, endTime, isFlexible).
     *                 isFlexible=true durumunda startTime/endTime placeholder
     *                 ("00:00"/"23:59") taşır; gerçek window'u use case
     *                 [resolveFlexibleWindow] ile yeniden hesaplar.
     */
    fun setOnFavoriteTimeSelectedListener(listener: (Date, String?, String?, Boolean, Double?) -> Unit) {
        onFavoriteTimeSelectedListener = listener
    }

    /**
     * Set listener for step-edit time selection (used when changing the time
     * of an existing activity-type timeline step).
     * @param listener Callback with (selectedDate, startTime HH:mm, endTime HH:mm or null,
     *                 selectedSlotPrice or null). The price drives the change-time
     *                 price update (null leaves the price as is).
     */
    fun setOnStepTimeSelectedListener(listener: (Date, String, String?, Double?) -> Unit) {
        onStepTimeSelectedListener = listener
    }

    /**
     * Set listener for the "Remove" action (SavedPlans flow only). The host is
     * responsible for showing the confirmation alert and performing the removal.
     */
    fun setOnRemoveListener(listener: () -> Unit) {
        onRemoveListener = listener
    }

    /**
     * Shows an inline loader inside this sheet while the host performs the
     * operation. Rendered by [BaseBottomDialogFragment] as an overlay over the
     * sheet's own view tree (no separate window).
     */
    fun showInSheetLoadingOverlay(languageKey: String, fallback: String) {
        viewModel.showInSheetLoader(languageKey, fallback)
    }

    /** Hides the inline loading overlay (e.g. on failure/retry). */
    fun hideInSheetLoadingOverlay() {
        viewModel.hideLottieLoading()
    }

    /**
     * Surfaces an error over this sheet. Anchors [BottomToast] to the sheet's own
     * dialog window so it appears on top of the sheet rather than behind it on the
     * activity's content view.
     */
    fun showError(message: String) {
        if (!isAdded) return
        val parent = dialog?.window?.decorView as? ViewGroup
        BottomToast.show(
            activity = requireActivity(),
            message = message,
            alertType = AlertType.ERROR,
            parent = parent
        )
    }

    companion object {
        const val TAG = "ActivityTimeSelectionBottomSheet"
        private const val MAX_SLOT_SCROLL_HEIGHT_RATIO = 0.4f
        private const val DISMISS_SLIDE_THRESHOLD = 0.7f
        private const val ARG_ACTIVITY = "activity"
        private const val ARG_AVAILABLE_DAYS = "available_days"
        private const val ARG_INITIAL_SELECTED_DAY = "initial_selected_day"
        private const val ARG_FAVORITE_MODE = "favorite_mode"
        private const val ARG_FAVORITE_ACTIVITY_ID = "favorite_activity_id"
        private const val ARG_FAVORITE_CITY_ID = "favorite_city_id"
        private const val ARG_FAVORITE_TITLE = "favorite_title"
        private const val ARG_FAVORITE_DURATION = "favorite_duration"
        private const val ARG_STEP_EDIT_MODE = "step_edit_mode"
        private const val ARG_NOT_AVAILABLE = "not_available"
        private const val ARG_INITIAL_TIME_SLOT = "initial_time_slot"
        private const val ARG_SHOW_SELECT_AND_REMOVE = "show_select_and_remove"
        private const val ARG_HIDE_DAY_SELECTOR = "hide_day_selector"
        private const val ARG_PLANNED_ACTIVITY_IDS = "planned_activity_ids"

        /**
         * Create instance for TourProduct (with API schedule loading)
         * @param cityId used only to resolve the timezone for the past-slot check.
         * @param plannedActivityIdsByDay "yyyy-MM-dd" → ids that day already holds. Days
         *   holding this activity render unselectable; edit flows pass none.
         */
        fun newInstance(
            activity: TourProduct,
            availableDays: List<Date>,
            initialSelectedDay: Date? = null,
            cityId: Int? = null,
            plannedActivityIdsByDay: Map<String, List<String>> = emptyMap()
        ): ActivityTimeSelectionBottomSheet {
            return ActivityTimeSelectionBottomSheet().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_ACTIVITY, activity)
                    putSerializable(ARG_AVAILABLE_DAYS, ArrayList(availableDays))
                    initialSelectedDay?.let { putSerializable(ARG_INITIAL_SELECTED_DAY, it) }
                    cityId?.let { putInt(ARG_FAVORITE_CITY_ID, it) }
                    putSerializable(
                        ARG_PLANNED_ACTIVITY_IDS,
                        plannedActivityIdsByDay.toSerializableIdsByDay()
                    )
                }
            }
        }

        /**
         * Create instance for SegmentFavoriteItem (with API schedule loading using activityId)
         * @param showSelectAndRemove SavedPlans flow: show "Select" primary + outlined "Remove".
         */
        fun newInstanceForFavorite(
            favoriteActivityId: String?,
            favoriteCityId: Int?,
            favoriteTitle: String,
            favoriteDuration: Double?,
            availableDays: List<Date>,
            initialSelectedDay: Date? = null,
            showSelectAndRemove: Boolean = false
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
                    putBoolean(ARG_SHOW_SELECT_AND_REMOVE, showSelectAndRemove)
                }
            }
        }

        /**
         * Create instance for changing the time of an existing activity-type
         * timeline step. Reuses the favorite-mode schedule loading path
         * (activityId + cityId) and seeds the slot grid with the step's
         * current HH:mm.
         *
         * @param hideDaySelector `true` hides the day filter row entirely, restricting
         *   the sheet to [initialSelectedDay]'s time slots only.
         */
        fun newInstanceForStepEdit(
            activityId: String?,
            cityId: Int?,
            title: String,
            duration: Double?,
            availableDays: List<Date>,
            initialSelectedDay: Date? = null,
            initialTimeSlot: String? = null,
            isNotAvailable: Boolean = false,
            hideDaySelector: Boolean = false
        ): ActivityTimeSelectionBottomSheet {
            return ActivityTimeSelectionBottomSheet().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_STEP_EDIT_MODE, true)
                    putBoolean(ARG_NOT_AVAILABLE, isNotAvailable)
                    putBoolean(ARG_HIDE_DAY_SELECTOR, hideDaySelector)
                    activityId?.let { putString(ARG_FAVORITE_ACTIVITY_ID, it) }
                    cityId?.let { putInt(ARG_FAVORITE_CITY_ID, it) }
                    putString(ARG_FAVORITE_TITLE, title)
                    duration?.let { putDouble(ARG_FAVORITE_DURATION, it) }
                    putSerializable(ARG_AVAILABLE_DAYS, ArrayList(availableDays))
                    initialSelectedDay?.let { putSerializable(ARG_INITIAL_SELECTED_DAY, it) }
                    initialTimeSlot?.let { putString(ARG_INITIAL_TIME_SLOT, it) }
                }
            }
        }
    }
}
