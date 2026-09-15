package com.tripian.trpcore.ui.timeline.compose.savedplans

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.BundleCompat
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.AcSavedPlansBinding
import com.tripian.trpcore.domain.model.itinerary.SegmentFavoriteItem
import com.tripian.trpcore.ui.timeline.compose.core.BindingHost
import com.tripian.trpcore.ui.timeline.compose.core.TimelineComposeScreen
import com.tripian.trpcore.ui.timeline.compose.core.timelineViewModel
import com.tripian.trpcore.ui.timeline.compose.nav.LocalTimelineNavigator
import com.tripian.trpcore.ui.timeline.compose.nav.SavedPlansArgs
import com.tripian.trpcore.ui.timeline.compose.nav.TimelineNavigator
import com.tripian.trpcore.ui.timeline.compose.nav.TimelineResults
import com.tripian.trpcore.ui.timeline.compose.sheets.ActivityTimeSelectionMode
import com.tripian.trpcore.ui.timeline.compose.sheets.ActivityTimeSelectionRequest
import com.tripian.trpcore.ui.timeline.compose.sheets.ActivityTimeSelectionSheet
import com.tripian.trpcore.ui.timeline.savedplans.ACSavedPlans
import com.tripian.trpcore.ui.timeline.savedplans.ACSavedPlansVM
import com.tripian.trpcore.ui.timeline.savedplans.AdapterSavedPlans
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.dialog.DGActionListener
import com.tripian.trpcore.util.extensions.applyBottomSystemBarInsetPadding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val STATE_LIST = "state_list"

/**
 * Compose route of [ACSavedPlans]: lists the trip's saved plans grouped by city
 * and adds them to the itinerary through the time selection sheet. Flags
 * [TimelineResults.SAVED_PLANS_CHANGED] for the Timeline whenever a plan was
 * added or removed.
 */
@Composable
internal fun SavedPlansScreen(
    args: SavedPlansArgs,
    viewModel: ACSavedPlansVM = timelineViewModel()
) {
    val navigator = LocalTimelineNavigator.current
    val context = LocalContext.current
    val pendingAdd = remember { PendingAddedFavorite() }
    val sheet = remember { SavedPlansSheetState() }

    LaunchedEffect(Unit) {
        if (viewModel.arguments == null) {
            viewModel.arguments = ACSavedPlans.launch(
                context = context,
                favorites = args.favorites,
                tripHash = args.tripHash,
                availableDays = args.availableDays,
                cityNameToIdMap = args.cityNameToIdMap,
                plannedActivityIdsByDay = args.plannedActivityIdsByDay
            ).extras
            viewModel.onViewCreated(null)
            viewModel.initialize(
                favorites = args.favorites,
                tripHash = args.tripHash,
                availableDays = args.availableDays,
                cityNameToIdMap = args.cityNameToIdMap,
                plannedActivityIdsByDay = args.plannedActivityIdsByDay
            )
        }
    }

    TimelineComposeScreen(viewModel = viewModel, onExit = { navigator.back() }) {
        BindingHost(
            inflate = AcSavedPlansBinding::inflate,
            onSaveViewState = { binding, state ->
                binding.rvSavedPlans.layoutManager?.onSaveInstanceState()
                    ?.let { state.putParcelable(STATE_LIST, it) }
            }
        ) { binding, owner, viewState ->
            val adapter = AdapterSavedPlans(
                getLanguage = { key -> viewModel.getLanguageForKey(key) },
                onAddClicked = { favorite -> viewModel.onActivityAddClicked(favorite) },
                onItemClicked = { favorite ->
                    favorite.activityId?.let { TRPCore.notifyActivityDetailRequested(it) }
                }
            )
            binding.rvSavedPlans.applyBottomSystemBarInsetPadding()
            binding.rvSavedPlans.layoutManager = LinearLayoutManager(binding.root.context)
            binding.rvSavedPlans.adapter = adapter
            binding.applyTexts(viewModel)
            binding.ivBack.setOnClickListener { navigator.back() }
            binding.btnViewItinerary.setOnClickListener {
                navigator.finishWithResult(TimelineResults.SAVED_PLANS_CHANGED, true)
            }
            binding.observe(adapter, viewModel, owner, viewState, navigator, sheet, pendingAdd)
        }
    }

    val request = sheet.request
    val favorite = sheet.favorite
    if (request != null && favorite != null) {
        ActivityTimeSelectionSheet(
            request = request,
            inSheetLoadingText = sheet.loadingText,
            onDismiss = { sheet.close() },
            onTimeSelected = { selection ->
                pendingAdd.name = favorite.title
                pendingAdd.date = selection.selectedDate
                sheet.loadingText = viewModel.getLanguageForKey(LanguageConst.LOADING_TEXT_ADDING_TO_ITINERARY)
                viewModel.createReservedActivitySegment(
                    selection.selectedDate,
                    selection.startTime,
                    selection.endTime,
                    selection.isFlexible,
                    selection.slotPrice
                )
            },
            onRemove = { showRemoveConfirmation(favorite, viewModel) }
        )
    }
}

private class PendingAddedFavorite {
    var name: String? = null
    var date: Date? = null
}

/** The time selection sheet currently shown for a favorite, if any, and its in-sheet loader. */
private class SavedPlansSheetState {
    var request by mutableStateOf<ActivityTimeSelectionRequest?>(null)
    var favorite by mutableStateOf<SegmentFavoriteItem?>(null)
    var loadingText by mutableStateOf<String?>(null)

    fun open(favorite: SegmentFavoriteItem, request: ActivityTimeSelectionRequest) {
        this.favorite = favorite
        this.request = request
        loadingText = null
    }

    fun close() {
        request = null
        favorite = null
        loadingText = null
    }
}

private fun AcSavedPlansBinding.applyTexts(viewModel: ACSavedPlansVM) {
    tvTitle.text = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_SAVED_PLANS)
    tvAllSetTitle.text = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_ALL_ADDED_TITLE)
    tvAllSetDescription.text = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_ALL_ADDED_DESCRIPTION)
    btnViewItinerary.text = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_VIEW_ITINERARY)
}

private fun AcSavedPlansBinding.observe(
    adapter: AdapterSavedPlans,
    viewModel: ACSavedPlansVM,
    owner: LifecycleOwner,
    viewState: Bundle,
    navigator: TimelineNavigator,
    sheet: SavedPlansSheetState,
    pendingAdd: PendingAddedFavorite
) {
    viewModel.listItems.observe(owner) { items ->
        adapter.submitList(items) { restoreListState(viewState) }
        val isEmpty = items.isEmpty()
        emptyStateContainer.visibility = if (isEmpty) View.VISIBLE else View.GONE
        rvSavedPlans.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
    viewModel.showTimeSelection.observe(owner) { favorite ->
        favorite?.let { showTimeSelectionSheet(it, viewModel, sheet) }
    }
    viewModel.segmentCreated.observe(owner) { created ->
        if (created) {
            viewModel.resetSegmentCreated()
            sheet.close()
            showAddedToItinerarySuccess(viewModel, pendingAdd)
            navigator.setResult(TimelineResults.SAVED_PLANS_CHANGED, true)
        }
    }
    viewModel.segmentCreationFailed.observe(owner) { failed ->
        failed?.let {
            viewModel.resetSegmentCreationFailed()
            sheet.loadingText = null
        }
    }
    viewModel.favoriteRemoved.observe(owner) { removed ->
        removed?.let {
            viewModel.resetFavoriteRemoved()
            sheet.close()
            navigator.setResult(TimelineResults.SAVED_PLANS_CHANGED, true)
        }
    }
}

private fun AcSavedPlansBinding.restoreListState(viewState: Bundle) {
    val state = BundleCompat.getParcelable(viewState, STATE_LIST, Parcelable::class.java) ?: return
    viewState.remove(STATE_LIST)
    rvSavedPlans.layoutManager?.onRestoreInstanceState(state)
}

/**
 * Opens the time selection sheet for [favorite]; schedule loading happens in
 * the sheet's own ViewModel.
 */
private fun showTimeSelectionSheet(
    favorite: SegmentFavoriteItem,
    viewModel: ACSavedPlansVM,
    sheet: SavedPlansSheetState
) {
    viewModel.clearTimeSelectionTrigger()
    val resolvedCityId = favorite.cityId?.takeIf { it > 0 }
        ?: viewModel.getResolvedCityId(favorite.cityName)
    sheet.open(
        favorite = favorite,
        request = ActivityTimeSelectionRequest(
            mode = ActivityTimeSelectionMode.Favorite(
                activityId = favorite.activityId,
                cityId = resolvedCityId,
                title = favorite.title,
                duration = favorite.duration,
                showSelectAndRemove = true,
                plannedActivityIdsByDay = viewModel.getPlannedActivityIdsByDay()
            ),
            availableDays = viewModel.getAvailableDays(),
            initialSelectedDay = viewModel.getSelectedDate()
        )
    )
}

private fun showAddedToItinerarySuccess(viewModel: ACSavedPlansVM, pendingAdd: PendingAddedFavorite) {
    val name = pendingAdd.name ?: return
    val date = pendingAdd.date ?: return
    val dayLabel = SimpleDateFormat("EEEE dd/MM", Locale.getDefault()).format(date)
    val message = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_TOAST_ACTIVITY_ADDED)
        .replace("%1\$@", name)
        .replace("%2\$@", dayLabel)
    viewModel.showAlert(AlertType.SUCCESS, message)
}

private fun showRemoveConfirmation(favorite: SegmentFavoriteItem, viewModel: ACSavedPlansVM) {
    viewModel.showDialog(
        title = viewModel.getLanguageForKey(LanguageConst.REMOVE_ACTIVITY),
        contentText = viewModel.getLanguageForKey(LanguageConst.REMOVE_ACTIVITY_MESSAGE),
        positiveBtn = viewModel.getLanguageForKey(LanguageConst.REMOVE_BUTTON),
        negativeBtn = viewModel.getLanguageForKey(LanguageConst.CANCEL),
        positive = object : DGActionListener {
            override fun onClicked(o: Any?) {
                viewModel.removeFavorite(favorite)
            }
        },
        isCloseEnable = false
    )
}
