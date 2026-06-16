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
    private var currentFlexiblePrice: Double? = null
    private var isFlexibleSelected: Boolean = false

    private var onTimeSelectedListener: ((TourProduct, Date, String, Double?, Boolean) -> Unit)? = null

    // SavedPlans flow: primary button becomes "Select" and an outlined "Remove"
    // button is shown (always enabled). The normal activity-listing flow keeps
    // the single "Continue" button.
    private var showSelectAndRemove: Boolean = false
    private var onRemoveListener: (() -> Unit)? = null

    // For favorites mode (uses activityId for schedule API)
    private var isFavoriteMode: Boolean = false
    private var favoriteActivityId: String? = null
    private var favoriteCityId: Int? = null
    private var favoriteTitle: String? = null
    private var favoriteDuration: Double? = null
    // Last param is the selected slot's min price (null when the slot carries no
    // price); the caller uses it to set the new segment's price.
    private var onFavoriteTimeSelectedListener: ((Date, String?, String?, Boolean, Double?) -> Unit)? = null

    // Step-edit mode shares the favorite schedule load path (activityId + cityId)
    // but routes the confirm action to a different callback that returns HH:mm
    // start/end so the caller can patch the existing step's time. We also seed
    // the time grid with the step's current slot on first render.
    private var isStepEditMode: Boolean = false
    private var pendingInitialTimeSlot: String? = null
    // Last param is the selected slot's min price (null when the slot carries no
    // price); the caller uses it to update the activity's price on change-time.
    private var onStepTimeSelectedListener: ((Date, String, String?, Double?) -> Unit)? = null

    /**
     * Original booked time (`HH:mm`) kept beyond [pendingInitialTimeSlot]'s
     * one-shot pre-selection so we can render it as a disabled chip when the
     * user is on the step's original day and the schedule no longer offers it.
     * Null in non-step-edit modes.
     */
    private var initialTimeSlot: String? = null

    /**
     * `yyyy-MM-dd` key of the step's original day. The disabled chip should
     * only surface when the day filter is on this date — switching to another
     * day clears the visual cue automatically because the keys mismatch.
     */
    private var initialDayKey: String? = null

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
            isStepEditMode = args.getBoolean(ARG_STEP_EDIT_MODE, false)
            showSelectAndRemove = args.getBoolean(ARG_SHOW_SELECT_AND_REMOVE, false)
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

        // Replace day filter + slot grid with a single warning card when the
        // trip range has zero availability for this activity.
        viewModel.isUnavailableForTrip.observe(viewLifecycleOwner) { unavailable ->
            applyTripUnavailableState(unavailable)
        }
    }

    private fun applyTripUnavailableState(unavailable: Boolean) {
        if (unavailable) {
            // Day filter stays visible (every cell disabled via empty
            // availableDateStrings). Only the slot grid area is replaced.
            binding.tripUnavailableCard.visibility = View.VISIBLE
            binding.tvSelectTime.visibility = View.GONE
            binding.flexTimeSlots.visibility = View.GONE
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
        // Set localized texts
        updateTexts()
        // Remove button is only part of the SavedPlans flow
        binding.btnRemove.visibility = if (showSelectAndRemove) View.VISIBLE else View.GONE
        // Update continue button state
        updateContinueButtonState()
    }

    private fun updateTexts() {
        binding.tvTitle.text = if (isStepEditMode) {
            getLanguageForKey(LanguageConst.CHANGE_TIME)
        } else {
            getLanguageForKey(LanguageConst.ADD_PLAN_TITLE)
        }
        binding.tvAddToDay.text = getLanguageForKey(LanguageConst.ADD_PLAN_ADD_TO_DAY)
        binding.tvSelectTime.text = getLanguageForKey(LanguageConst.ADD_PLAN_SELECT_A_TIME)
        binding.tvNoTimeSlots.text = getLanguageForKey(LanguageConst.ADD_PLAN_NO_TIME_SLOTS)
        binding.tvTripUnavailable.text = getLanguageForKey(LanguageConst.ADD_PLAN_ACTIVITY_NOT_AVAILABLE_TRIP_DAYS)
        // SavedPlans flow uses "Select" as the primary CTA; everything else
        // keeps the "Continue" label.
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

        // Remove button - SavedPlans flow only. Confirmation + removal are
        // handled by the host (it shows the alert and performs the removal).
        // Independent of any time-slot selection, so always actionable.
        binding.btnRemove.setOnClickListener {
            onRemoveListener?.invoke()
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
                onFavoriteTimeSelectedListener?.invoke(date, "00:00", "23:59", true, currentFlexiblePrice)
            } else {
                val tour = activity ?: return
                onTimeSelectedListener?.invoke(tour, date, "00:00", currentFlexiblePrice, true)
            }
            return
        }

        val timeSlot = selectedTimeSlot ?: return

        if (isStepEditMode) {
            // Step-edit emits HH:mm start/end for the host VM to patch via
            // updateStepTime. End time is computed from the step's stored
            // duration (same recipe as the favorite path). selectedPrice is the
            // chosen slot's price so the activity price can follow the new time.
            val endTime = calculateEndTimeFromDuration(timeSlot, favoriteDuration)
            onStepTimeSelectedListener?.invoke(date, timeSlot, endTime, selectedPrice)
        } else if (isFavoriteMode) {
            // For favorites - calculate end time from duration
            val endTime = calculateEndTimeFromDuration(timeSlot, favoriteDuration)
            onFavoriteTimeSelectedListener?.invoke(date, timeSlot, endTime, false, selectedPrice)
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
        // Step-edit shares the favorite-mode load path: it already has an
        // activityId + cityId in hand (from the existing step's POI).
        val activityId = if (isFavoriteMode || isStepEditMode) {
            favoriteActivityId
        } else {
            activity?.id
        }

        if (activityId == null) return
        if (availableDays.isEmpty()) return
        val selectedDate = availableDays.getOrNull(selectedDayIndex) ?: availableDays.first()

        // For favorites and step-edit, pass cityId for proper activityId formatting
        val cityId = if (isFavoriteMode || isStepEditMode) favoriteCityId else null
        viewModel.loadSchedule(activityId, availableDays, selectedDate, cityId)
    }

    /**
     * Render whichever combination of (time grid / flexible info card /
     * empty-state label) is appropriate for the resolved schedule. See
     * [TimeSelectionMode] for the three shapes.
     */
    private fun updateSchedule(resolved: ResolvedSchedule?) {
        // Trip-wide unavailability takes over the whole sheet — skip per-day
        // grid/flexible-card rendering so the warning card isn't overridden by
        // a late `resolvedSchedule` emission.
        if (viewModel.isUnavailableForTrip.value == true) return

        val safe = resolved ?: ResolvedSchedule(
            mode = TimeSelectionMode.TIMED,
            timedSlots = emptyList(),
            flexiblePrice = null
        )

        currentFlexiblePrice = safe.flexiblePrice
        currentSlots = safe.timedSlots
        // Switching days/modes resets any previous selection.
        selectedTimeSlot = null
        selectedPrice = null
        isFlexibleSelected = false

        // Step-edit mode: the very first render for the step's original day
        // re-applies the step's current HH:mm if it still exists in the slot
        // grid. Consumed on first match so day switches don't keep forcing it.
        pendingInitialTimeSlot?.let { initialSlot ->
            val match = currentSlots.firstOrNull { it.time == initialSlot }
            if (match != null) {
                selectedTimeSlot = match.time
                selectedPrice = match.minPrice
            }
            pendingInitialTimeSlot = null
        }

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
            TimeSelectionMode.TIMED -> {
                binding.tvSelectTime.visibility = View.VISIBLE
                binding.flexibleInfoCard.visibility = View.GONE
                binding.tvFlexibleTopOfItinerary.visibility = View.GONE

                if (currentSlots.isEmpty()) {
                    // No available timed slots for the selected day → "not available" warning.
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

        // Build the full chip set: available slots + (optionally) the disabled
        // chip representing the step's originally-booked time that the
        // backend no longer offers. The disabled chip only appears in
        // step-edit mode AND when the day filter is on the step's original
        // day — switching days hides it automatically because the day keys
        // mismatch. Both kinds render in chronological order.
        val currentDayKey = availableDays.getOrNull(selectedDayIndex)?.let {
            dayKeyFormatter.format(it)
        }
        val disabledTime = initialTimeSlot
            ?.takeIf {
                isStepEditMode &&
                    currentDayKey != null &&
                    currentDayKey == initialDayKey &&
                    slots.none { slot -> slot.time == it }
            }

        data class ChipSpec(val time: String, val price: Double?, val isDisabled: Boolean)
        val chips = mutableListOf<ChipSpec>()
        chips += slots.map { ChipSpec(it.time, it.minPrice, isDisabled = false) }
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
                // Solid-gray background + white text; chip is non-interactive
                // because the backend has confirmed this slot is no longer
                // available. The selector drawable from the layout is replaced
                // wholesale so selected/activated states cannot kick in.
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

            // Set layout params for FlexboxLayout with fixed width for 4 columns
            val params = com.google.android.flexbox.FlexboxLayout.LayoutParams(
                itemWidthPx,
                heightPx
            )
            params.setMargins(0, 0, marginPx, marginPx)
            chipView.layoutParams = params

            binding.flexTimeSlots.addView(chipView)
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
     *                 price update (kept unchanged when null).
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
     * Shows an inline loader inside this sheet (e.g. "Adding to itinerary" for
     * the SavedPlans add flow, "Changing time" for the change-time flow) while
     * the host performs the operation. Rendered by [BaseBottomDialogFragment] as
     * an overlay over the sheet's own view tree (no separate window), unlike a
     * full-screen/bottom-sheet loader dialog.
     */
    fun showInSheetLoadingOverlay(languageKey: String, fallback: String) {
        viewModel.showInSheetLoader(languageKey, fallback)
    }

    /** Hides the inline loading overlay (e.g. on failure/retry). */
    fun hideInSheetLoadingOverlay() {
        viewModel.hideLottieLoading()
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
        private const val ARG_STEP_EDIT_MODE = "step_edit_mode"
        private const val ARG_INITIAL_TIME_SLOT = "initial_time_slot"
        private const val ARG_SHOW_SELECT_AND_REMOVE = "show_select_and_remove"

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
            initialSelectedDay: Date? = null,
            // SavedPlans flow: show "Select" primary + outlined "Remove".
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
         */
        fun newInstanceForStepEdit(
            activityId: String?,
            cityId: Int?,
            title: String,
            duration: Double?,
            availableDays: List<Date>,
            initialSelectedDay: Date? = null,
            initialTimeSlot: String? = null
        ): ActivityTimeSelectionBottomSheet {
            return ActivityTimeSelectionBottomSheet().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_STEP_EDIT_MODE, true)
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
