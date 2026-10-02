package com.tripian.trpcore.ui.timeline.compose.sheets

import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.flexbox.FlexboxLayout
import com.tripian.one.api.tour.model.TourProduct
import com.tripian.trpcore.R
import com.tripian.trpcore.databinding.BottomSheetActivityTimeSelectionBinding
import com.tripian.trpcore.ui.common.loader.LottieLoadingText
import com.tripian.trpcore.ui.timeline.activity.ActivityTimeSelectionBottomSheet
import com.tripian.trpcore.ui.timeline.activity.ActivityTimeSelectionVM
import com.tripian.trpcore.ui.timeline.activity.GroupedTimeSlot
import com.tripian.trpcore.ui.timeline.activity.ResolvedSchedule
import com.tripian.trpcore.ui.timeline.activity.TimeSelectionMode
import com.tripian.trpcore.ui.timeline.adapter.DayFilterAdapter
import com.tripian.trpcore.ui.timeline.compose.core.BindViewListener
import com.tripian.trpcore.ui.timeline.compose.core.BindingHost
import com.tripian.trpcore.ui.timeline.compose.core.TimelineLoaderOverlay
import com.tripian.trpcore.ui.timeline.compose.core.TimelineSheet
import com.tripian.trpcore.ui.timeline.compose.core.rememberSheetViewModel
import com.tripian.trpcore.util.CityTimeZones
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.hideSheetDragHandle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Which item the activity time sheet schedules; mirrors the three fragment factories. */
sealed interface ActivityTimeSelectionMode {

    data class Tour(
        val activity: TourProduct,
        val cityId: Int?,
        val plannedActivityIdsByDay: Map<String, List<String>> = emptyMap()
    ) : ActivityTimeSelectionMode

    data class Favorite(
        val activityId: String?,
        val cityId: Int?,
        val title: String,
        val duration: Double?,
        val showSelectAndRemove: Boolean = false,
        val plannedActivityIdsByDay: Map<String, List<String>> = emptyMap()
    ) : ActivityTimeSelectionMode

    data class StepEdit(
        val activityId: String?,
        val cityId: Int?,
        val title: String,
        val duration: Double?,
        val initialTimeSlot: String? = null,
        val isNotAvailable: Boolean = false,
        val hideDaySelector: Boolean = false
    ) : ActivityTimeSelectionMode
}

data class ActivityTimeSelectionRequest(
    val mode: ActivityTimeSelectionMode,
    val availableDays: List<Date>,
    val initialSelectedDay: Date? = null
)

/**
 * The slot the user confirmed. A flexible pick reports 00:00 to 23:59 with
 * [isFlexible] true; [endTime] is null when the sheet has no end for the slot.
 */
data class ActivityTimeSelection(
    val selectedDate: Date,
    val startTime: String,
    val endTime: String?,
    val isFlexible: Boolean,
    val slotPrice: Double?
)

private const val SLOT_HEIGHT_DP = 36
private const val SLOT_MARGIN_DP = 8
private const val SLOT_GRID_HORIZONTAL_MARGIN_DP = 24
private const val SLOT_COLUMN_COUNT = 4
private const val FLEXIBLE_START = "00:00"
private const val FLEXIBLE_END = "23:59"

/**
 * Compose counterpart of ActivityTimeSelectionBottomSheet: day filter, time
 * slots (or flexible), and the Continue / Select & Remove footer. Shown as a
 * [com.tripian.trpcore.ui.timeline.compose.core.TimelineSheet].
 *
 * @param inSheetLoadingText non-null shows the in-sheet loader with that text ("" for no text)
 *   and locks the sheet until the host clears it
 * @param onRemove non-null shows the Remove action (favorites and step edits)
 */
@Composable
internal fun ActivityTimeSelectionSheet(
    request: ActivityTimeSelectionRequest,
    inSheetLoadingText: String?,
    onDismiss: () -> Unit,
    onTimeSelected: (ActivityTimeSelection) -> Unit,
    onRemove: (() -> Unit)? = null
) {
    val viewModel: ActivityTimeSelectionVM = rememberSheetViewModel()
    BindViewListener(viewModel, onExit = onDismiss)
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val currentOnTimeSelected by rememberUpdatedState(onTimeSelected)
    val currentOnRemove by rememberUpdatedState(onRemove)
    val loaderEvent by viewModel.lottieLoadingEvent.observeAsState()
    val vmLoader = loaderEvent?.takeIf { it.show }
    val busy = inSheetLoadingText != null || vmLoader != null

    TimelineSheet(onDismissRequest = onDismiss, dismissible = !busy) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            BindingHost(
                inflate = BottomSheetActivityTimeSelectionBinding::inflate,
                modifier = Modifier.fillMaxWidth()
            ) { binding, owner, _ ->
                if (viewModel.arguments == null) {
                    viewModel.arguments = request.toFragmentArguments()
                    viewModel.onViewCreated(null)
                    viewModel.onStart()
                }
                ActivityTimeSelectionUi(
                    binding = binding,
                    viewModel = viewModel,
                    request = request,
                    onDismiss = { currentOnDismiss() },
                    onTimeSelected = { currentOnTimeSelected(it) },
                    onRemove = { currentOnRemove?.invoke() }
                ).bind(owner)
            }
        }
        when {
            inSheetLoadingText != null ->
                TimelineLoaderOverlay(LottieLoadingText.Single(inSheetLoadingText), Modifier.matchParentSize())
            vmLoader != null ->
                TimelineLoaderOverlay(vmLoader.text, Modifier.matchParentSize())
        }
    }
}

private fun ActivityTimeSelectionRequest.toFragmentArguments(): Bundle? = when (val m = mode) {
    is ActivityTimeSelectionMode.Tour -> ActivityTimeSelectionBottomSheet.newInstance(
        activity = m.activity,
        availableDays = availableDays,
        initialSelectedDay = initialSelectedDay,
        cityId = m.cityId,
        plannedActivityIdsByDay = m.plannedActivityIdsByDay
    ).arguments
    is ActivityTimeSelectionMode.Favorite -> ActivityTimeSelectionBottomSheet.newInstanceForFavorite(
        favoriteActivityId = m.activityId,
        favoriteCityId = m.cityId,
        favoriteTitle = m.title,
        favoriteDuration = m.duration,
        availableDays = availableDays,
        initialSelectedDay = initialSelectedDay,
        showSelectAndRemove = m.showSelectAndRemove,
        plannedActivityIdsByDay = m.plannedActivityIdsByDay
    ).arguments
    is ActivityTimeSelectionMode.StepEdit -> ActivityTimeSelectionBottomSheet.newInstanceForStepEdit(
        activityId = m.activityId,
        cityId = m.cityId,
        title = m.title,
        duration = m.duration,
        availableDays = availableDays,
        initialSelectedDay = initialSelectedDay,
        initialTimeSlot = m.initialTimeSlot,
        isNotAvailable = m.isNotAvailable,
        hideDaySelector = m.hideDaySelector
    ).arguments
}

/**
 * View-side controller of one inflated sheet layout: ports the fragment's
 * selection state, day filter, slot grid and footer onto [binding].
 */
private class ActivityTimeSelectionUi(
    private val binding: BottomSheetActivityTimeSelectionBinding,
    private val viewModel: ActivityTimeSelectionVM,
    request: ActivityTimeSelectionRequest,
    private val onDismiss: () -> Unit,
    private val onTimeSelected: (ActivityTimeSelection) -> Unit,
    private val onRemove: () -> Unit
) {
    private val context = binding.root.context
    private val dayKeyFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val availableDays: List<Date> = request.initialSelectedDay
        ?.takeIf { initial -> request.availableDays.none { isSameDay(it, initial) } }
        ?.let { initial -> (request.availableDays + initial).sortedBy { it.time } }
        ?: request.availableDays
    private val mode = request.mode
    private val activity: TourProduct? = (mode as? ActivityTimeSelectionMode.Tour)?.activity
    private val isFavoriteMode = mode is ActivityTimeSelectionMode.Favorite
    private val isStepEditMode = mode is ActivityTimeSelectionMode.StepEdit
    private val favoriteActivityId: String? = when (mode) {
        is ActivityTimeSelectionMode.Favorite -> mode.activityId
        is ActivityTimeSelectionMode.StepEdit -> mode.activityId
        is ActivityTimeSelectionMode.Tour -> null
    }
    private val favoriteCityId: Int? = when (mode) {
        is ActivityTimeSelectionMode.Favorite -> mode.cityId
        is ActivityTimeSelectionMode.StepEdit -> mode.cityId
        is ActivityTimeSelectionMode.Tour -> mode.cityId
    }?.takeIf { it > 0 }
    private val favoriteDuration: Double? = when (mode) {
        is ActivityTimeSelectionMode.Favorite -> mode.duration
        is ActivityTimeSelectionMode.StepEdit -> mode.duration
        is ActivityTimeSelectionMode.Tour -> null
    }?.takeIf { it > 0 }
    private val showSelectAndRemove =
        (mode as? ActivityTimeSelectionMode.Favorite)?.showSelectAndRemove == true
    private val isActivityNotAvailable =
        (mode as? ActivityTimeSelectionMode.StepEdit)?.isNotAvailable == true
    private val hideDaySelector =
        (mode as? ActivityTimeSelectionMode.StepEdit)?.hideDaySelector == true
    private val plannedActivityIdsByDay: Map<String, List<String>> = when (mode) {
        is ActivityTimeSelectionMode.Favorite -> mode.plannedActivityIdsByDay
        is ActivityTimeSelectionMode.Tour -> mode.plannedActivityIdsByDay
        is ActivityTimeSelectionMode.StepEdit -> emptyMap()
    }
    private val initialTimeSlot: String? = (mode as? ActivityTimeSelectionMode.StepEdit)?.initialTimeSlot
    private var pendingInitialTimeSlot: String? = initialTimeSlot
    private val initialDayKey: String? = request.initialSelectedDay?.let { dayKeyFormatter.format(it) }

    private var dayAdapter: DayFilterAdapter? = null
    private var selectedDayIndex: Int = request.initialSelectedDay?.let { initial ->
        availableDays.indexOfFirst { isSameDay(it, initial) }.coerceAtLeast(0)
    } ?: 0
    private var selectedTimeSlot: String? = null
    private var selectedPrice: Double? = null
    private var currentSlots: List<GroupedTimeSlot> = emptyList()
    private var currentFlexiblePrice: Double? = null
    private var isFlexibleSelected: Boolean = false

    fun bind(owner: LifecycleOwner) {
        binding.root.hideSheetDragHandle()
        setupUI()
        setupDayFilter()
        setupClickListeners()
        observe(owner)
        requestScheduleLoad()
    }

    private fun observe(owner: LifecycleOwner) {
        viewModel.resolvedSchedule.observe(owner) { resolved -> updateSchedule(resolved) }
        viewModel.availableDateStrings.observe(owner) { available ->
            dayAdapter?.availableDateStrings = available
            jumpToFirstAvailableIfNeeded(available)
        }
        viewModel.isUnavailableForTrip.observe(owner) { unavailable ->
            applyTripUnavailableState(unavailable)
        }
    }

    private fun applyTripUnavailableState(unavailable: Boolean) {
        if (unavailable) {
            binding.tvTripUnavailable.text = viewModel.getLanguageForKey(viewModel.unavailableBannerKey())
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
            viewModel.getLanguageForKey(LanguageConst.CHANGE_TIME)
        } else {
            viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_TITLE)
        }
        binding.tvAddToDay.text = if (isStepEditMode && !isActivityNotAvailable) {
            viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_MOVE_DAY)
        } else {
            viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_ADD_TO_DAY)
        }
        binding.tvSelectTime.text = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_SELECT_A_TIME)
        binding.tvNoTimeSlots.text = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_NO_TIME_SLOTS)
        binding.tvTripUnavailable.text =
            viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_ACTIVITY_NOT_AVAILABLE_TRIP_DAYS)
        binding.btnContinue.text = if (showSelectAndRemove) {
            viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_SELECT)
        } else {
            viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_CONTINUE)
        }
        binding.btnRemove.text = viewModel.getLanguageForKey(LanguageConst.REMOVE_BUTTON)
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
        dayAdapter?.setDays(availableDays)
        dayAdapter?.setSelectedPosition(selectedDayIndex)
        binding.rvDays.scrollToPosition(selectedDayIndex)
    }

    private fun setupClickListeners() {
        binding.ivBack.setOnClickListener { onDismiss() }
        binding.btnContinue.setOnClickListener { addActivity() }
        binding.btnRemove.setOnClickListener { onRemove() }
    }

    private fun addActivity() {
        val date = availableDays.getOrNull(selectedDayIndex) ?: return

        if (isFlexibleSelected) {
            val endTime = if (activity != null) null else FLEXIBLE_END
            onTimeSelected(ActivityTimeSelection(date, FLEXIBLE_START, endTime, true, currentFlexiblePrice))
            return
        }

        val timeSlot = selectedTimeSlot ?: return
        val endTime = if (activity != null) null else calculateEndTimeFromDuration(timeSlot, favoriteDuration)
        onTimeSelected(ActivityTimeSelection(date, timeSlot, endTime, false, selectedPrice))
    }

    private fun calculateEndTimeFromDuration(startTime: String, duration: Double?): String? {
        if (duration == null || duration <= 0) return null
        return try {
            val parts = startTime.split(":")
            val totalMinutes = parts[0].toInt() * 60 + parts[1].toInt() + duration.toInt()
            String.format(Locale.US, "%02d:%02d", (totalMinutes / 60) % 24, totalMinutes % 60)
        } catch (e: Exception) {
            null
        }
    }

    private fun requestScheduleLoad() {
        val activityId = if (isFavoriteMode || isStepEditMode) favoriteActivityId else activity?.id
        if (activityId == null) return
        if (availableDays.isEmpty()) return
        val selectedDate = availableDays.getOrNull(selectedDayIndex) ?: availableDays.first()
        val cityId = if (isFavoriteMode || isStepEditMode) favoriteCityId else null
        viewModel.applyPlannedActivities(plannedActivityIdsByDay, activity?.productId ?: activityId)
        viewModel.loadSchedule(activityId, availableDays, selectedDate, cityId)
    }

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
                if (isStepEditMode) {
                    binding.tvTitle.text = viewModel.getLanguageForKey(LanguageConst.CHANGE_DAY)
                }
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
        binding.tvFlexibleTitle.text =
            viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_FLEXIBLE_INFO_TITLE)
        binding.tvFlexibleTopOfItinerary.text =
            viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_FLEXIBLE_TOP_OF_ITINERARY)
    }

    /**
     * Renders the slot chips in a fixed 4-column grid. Past slots for the city's
     * timezone are disabled; in step-edit mode on the step's original day, the
     * originally-booked time that the backend no longer offers is appended as a
     * disabled chip. All chips render in chronological order.
     */
    private fun populateTimeSlots(slots: List<GroupedTimeSlot>) {
        binding.flexTimeSlots.removeAllViews()

        val inflater = LayoutInflater.from(context)
        val density = context.resources.displayMetrics.density
        val heightPx = (SLOT_HEIGHT_DP * density).toInt()
        val marginPx = (SLOT_MARGIN_DP * density).toInt()
        val flexboxMarginPx = (SLOT_GRID_HORIZONTAL_MARGIN_DP * density * 2).toInt()
        val availableWidth = context.resources.displayMetrics.widthPixels - flexboxMarginPx
        val itemWidthPx = (availableWidth - SLOT_COLUMN_COUNT * marginPx) / SLOT_COLUMN_COUNT

        val currentDayKey = availableDays.getOrNull(selectedDayIndex)?.let { dayKeyFormatter.format(it) }
        val disabledTime = initialTimeSlot?.takeIf {
            isStepEditMode &&
                currentDayKey != null &&
                currentDayKey == initialDayKey &&
                currentSlots.none { slot -> slot.time == it }
        }
        val selectedDay = availableDays.getOrNull(selectedDayIndex)

        val chips = mutableListOf<ChipSpec>()
        chips += slots.map { slot ->
            val isPast = selectedDay != null &&
                CityTimeZones.isTimeSlotInPast(selectedDay, slot.time, favoriteCityId)
            ChipSpec(slot.time, slot.minPrice, isDisabled = isPast)
        }
        disabledTime?.let { chips += ChipSpec(it, price = null, isDisabled = true) }
        chips.sortBy { it.time }

        val disabledTextColor = ContextCompat.getColor(context, R.color.trp_white)

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
            chipView.layoutParams = slotLayoutParams(itemWidthPx, heightPx, marginPx)
            binding.flexTimeSlots.addView(chipView)
        }

        if (viewModel.shouldShowMoreCell) {
            val showMoreView = inflater.inflate(
                R.layout.item_time_slot_show_more,
                binding.flexTimeSlots,
                false
            ) as TextView
            showMoreView.text = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_TS_SHOW_MORE)
            showMoreView.paintFlags = showMoreView.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            showMoreView.setOnClickListener {
                viewModel.expandTimeSlots()
                populateTimeSlots(viewModel.getDisplayedSlots())
            }
            showMoreView.layoutParams = slotLayoutParams(itemWidthPx, heightPx, marginPx)
            binding.flexTimeSlots.addView(showMoreView)
        }
    }

    private fun slotLayoutParams(widthPx: Int, heightPx: Int, marginPx: Int): FlexboxLayout.LayoutParams =
        FlexboxLayout.LayoutParams(widthPx, heightPx).apply { setMargins(0, 0, marginPx, marginPx) }

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
        binding.btnContinue.isEnabled = isFlexibleSelected || selectedTimeSlot != null
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean =
        dayKeyFormatter.format(date1) == dayKeyFormatter.format(date2)

    private data class ChipSpec(val time: String, val price: Double?, val isDisabled: Boolean)
}
