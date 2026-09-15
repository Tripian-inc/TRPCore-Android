package com.tripian.trpcore.ui.timeline.compose.sheets

import androidx.compose.runtime.Composable
import com.tripian.one.api.tour.model.TourProduct
import java.util.Date

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

/**
 * Compose counterpart of ActivityTimeSelectionBottomSheet: day filter, time
 * slots (or flexible), and the Continue / Select & Remove footer. Shown as a
 * [com.tripian.trpcore.ui.timeline.compose.core.TimelineSheet].
 *
 * @param inSheetLoadingText non-null shows the in-sheet loader with that text ("" for no text)
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
}
