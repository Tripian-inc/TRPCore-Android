package com.tripian.trpcore.ui.timeline.compose.sheets

import androidx.compose.runtime.Composable
import java.util.Date

/**
 * Mirrors TimeSelectionBottomSheet.newInstance: [minTime] is the earliest
 * selectable "HH:mm", [defaultStartTime] prefills the start picker, and
 * [openingHours] with [selectedDay] drive the outside-hours warning.
 */
data class PoiTimeSelectionRequest(
    val startTime: String? = null,
    val endTime: String? = null,
    val minTime: String? = null,
    val defaultStartTime: String? = null,
    val openingHours: String? = null,
    val selectedDay: Date? = null
)

/**
 * Compose counterpart of TimeSelectionBottomSheet: start/end time pickers for
 * a POI step plus Confirm. Shown as a
 * [com.tripian.trpcore.ui.timeline.compose.core.TimelineSheet].
 *
 * @param inSheetLoadingText non-null shows the in-sheet loader with that text ("" for no text)
 */
@Composable
internal fun PoiTimeSelectionSheet(
    request: PoiTimeSelectionRequest,
    inSheetLoadingText: String?,
    onDismiss: () -> Unit,
    onConfirm: (startTime: String?, endTime: String?) -> Unit
) {
}
