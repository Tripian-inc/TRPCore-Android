package com.tripian.trpcore.ui.timeline.compose.sheets

import android.view.View
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.tripian.trpcore.R
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.BottomSheetTimeSelectionBinding
import com.tripian.trpcore.ui.common.loader.LottieLoadingText
import com.tripian.trpcore.ui.timeline.addplan.MaterialTimePickerHelper
import com.tripian.trpcore.ui.timeline.addplan.TimePickerDialogContent
import com.tripian.trpcore.ui.timeline.compose.core.BindingHost
import com.tripian.trpcore.ui.timeline.compose.core.TimelineLoaderOverlay
import com.tripian.trpcore.ui.timeline.compose.core.TimelineSheet
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.hideSheetDragHandle
import com.tripian.trpcore.util.OpeningHours
import com.tripian.trpcore.util.TimeSelectionValidation
import com.tripian.trpcore.util.extensions.applyTimeFieldError
import java.util.Date

/**
 * Mirrors TimeSelectionBottomSheet.newInstance: [minTime] is the exclusive
 * "HH:mm" floor in the city's clock (a start at or before it is flagged as
 * passed), [defaultStartTime] prefills the start picker, and [openingHours]
 * with [selectedDay] drive the outside-hours warning.
 */
data class PoiTimeSelectionRequest(
    val startTime: String? = null,
    val endTime: String? = null,
    val minTime: String? = null,
    val defaultStartTime: String? = null,
    val openingHours: String? = null,
    val selectedDay: Date? = null
)

private data class TimePickerRequest(
    val initialTime: String?,
    val treatMidnightAsEndOfDay: Boolean,
    val isEnd: Boolean
)

private class PoiTimeSelectionState(request: PoiTimeSelectionRequest) {
    var startTime by mutableStateOf(request.startTime)
    var endTime by mutableStateOf(request.endTime)
    var picker by mutableStateOf<TimePickerRequest?>(null)
}

/**
 * Compose counterpart of TimeSelectionBottomSheet: start/end time pickers for
 * a POI step plus Confirm. Shown as a
 * [com.tripian.trpcore.ui.timeline.compose.core.TimelineSheet].
 *
 * @param inSheetLoadingText non-null shows the in-sheet loader with that text ("" for no text)
 *   and locks the sheet until the host clears it
 */
@Composable
internal fun PoiTimeSelectionSheet(
    request: PoiTimeSelectionRequest,
    inSheetLoadingText: String?,
    onDismiss: () -> Unit,
    onConfirm: (startTime: String?, endTime: String?) -> Unit
) {
    val state = remember(request) { PoiTimeSelectionState(request) }
    val startTime = state.startTime
    val endTime = state.endTime

    TimelineSheet(onDismissRequest = onDismiss, dismissible = inSheetLoadingText == null) {
        BindingHost(
            inflate = BottomSheetTimeSelectionBinding::inflate,
            modifier = Modifier.fillMaxWidth(),
            update = { binding -> binding.render(request, startTime, endTime) }
        ) { binding, _, _ ->
            binding.root.hideSheetDragHandle()
            binding.applyLabels()
            binding.ivClose.setOnClickListener { onDismiss() }
            binding.llStartTime.setOnClickListener {
                state.picker = TimePickerRequest(
                    initialTime = state.startTime ?: request.defaultStartTime,
                    treatMidnightAsEndOfDay = false,
                    isEnd = false
                )
            }
            binding.llEndTime.setOnClickListener {
                state.picker = TimePickerRequest(
                    initialTime = endPickerInitialTime(state.startTime, state.endTime),
                    treatMidnightAsEndOfDay = true,
                    isEnd = true
                )
            }
            binding.btnConfirm.setOnClickListener { onConfirm(state.startTime, state.endTime) }
        }
        inSheetLoadingText?.let {
            TimelineLoaderOverlay(LottieLoadingText.Single(it), Modifier.matchParentSize())
        }
    }

    state.picker?.let { picker ->
        val initial = picker.initialTime?.let { MaterialTimePickerHelper.parseTime24h(it) }
        TimePickerDialogContent(
            initialHour = initial?.first ?: 10,
            initialMinute = initial?.second ?: 0,
            minHour = null,
            minMinute = null,
            treatMidnightAsEndOfDay = picker.treatMidnightAsEndOfDay,
            cancelText = language(LanguageConst.ADD_PLAN_CANCEL),
            selectText = language(LanguageConst.ADD_PLAN_SELECT),
            onConfirm = { hour, minute ->
                if (picker.isEnd) {
                    state.endTime = MaterialTimePickerHelper.formatEndTimeTo24h(hour, minute)
                } else {
                    state.startTime = MaterialTimePickerHelper.formatTo24h(hour, minute)
                }
                state.picker = null
            },
            onCancel = { state.picker = null }
        )
    }
}

/** A stored "23:59" end is the midnight sentinel, so the clock seeds at 00:00. */
private fun endPickerInitialTime(startTime: String?, endTime: String?): String? = when {
    endTime == MaterialTimePickerHelper.END_OF_DAY_24H -> "00:00"
    endTime != null -> endTime
    else -> MaterialTimePickerHelper.addMinutes(startTime, 60) ?: startTime
}

private fun language(key: String): String = TRPCore.core.miscRepository.getLanguageValueForKey(key)

private fun BottomSheetTimeSelectionBinding.applyLabels() {
    tvTitle.text = language(LanguageConst.ADD_PLAN_TIME)
    tvStartTimeLabel.text = language(LanguageConst.ADD_PLAN_START_TIME)
    tvEndTimeLabel.text = language(LanguageConst.ADD_PLAN_END_TIME)
    btnConfirm.text = language(LanguageConst.ADD_PLAN_CONFIRM)
}

private fun BottomSheetTimeSelectionBinding.render(
    request: PoiTimeSelectionRequest,
    startTime: String?,
    endTime: String?
) {
    val selectText = language(LanguageConst.ADD_PLAN_SELECT)
    val context = root.context
    tvStartTime.text = startTime?.let { MaterialTimePickerHelper.formatTo12h(it) } ?: selectText
    tvStartTime.setTextColor(
        context.getColor(if (startTime != null) R.color.trp_text_primary else R.color.trp_fgWeak)
    )
    tvEndTime.text = endTime?.let { MaterialTimePickerHelper.formatEndTimeTo12h(it) } ?: selectText
    tvEndTime.setTextColor(
        context.getColor(if (endTime != null) R.color.trp_text_primary else R.color.trp_fgWeak)
    )
    val errors = TimeSelectionValidation.validate(startTime, endTime, request.minTime)
    llStartTime.applyTimeFieldError(tvStartTimeError, errors.start)
    llEndTime.applyTimeFieldError(tvEndTimeError, errors.end)
    btnConfirm.isEnabled = startTime != null && endTime != null && errors.isValid
    renderClosedWarning(request, startTime, endTime)
}

/**
 * Shows a non-blocking notice when the picked span falls outside the POI's
 * opening hours for the planned day; stays hidden when the hours are unknown.
 */
private fun BottomSheetTimeSelectionBinding.renderClosedWarning(
    request: PoiTimeSelectionRequest,
    startTime: String?,
    endTime: String?
) {
    val isOpen = OpeningHours.coversSelection(request.openingHours, request.selectedDay, startTime, endTime)
    if (isOpen != false) {
        llClosedWarning.visibility = View.GONE
        return
    }
    val closedLabel = language(LanguageConst.ADD_PLAN_CLOSED_WARNING)
    val dayText = OpeningHours.dayText(request.openingHours, request.selectedDay)
    tvClosedWarning.text = if (dayText != null) {
        "$closedLabel\n${language(LanguageConst.ADD_PLAN_OPEN_HOURS)}: $dayText"
    } else {
        closedLabel
    }
    llClosedWarning.visibility = View.VISIBLE
}
