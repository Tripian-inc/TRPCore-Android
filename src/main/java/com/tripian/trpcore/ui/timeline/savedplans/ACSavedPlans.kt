package com.tripian.trpcore.ui.timeline.savedplans

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.AcSavedPlansBinding
import com.tripian.trpcore.domain.model.itinerary.SegmentFavoriteItem
import com.tripian.trpcore.ui.timeline.activity.ActivityTimeSelectionBottomSheet
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.dialog.DGActionListener
import com.tripian.trpcore.util.extensions.asIdsByDay
import com.tripian.trpcore.util.extensions.toSerializableIdsByDay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Full screen Activity for displaying saved plans (favorites) grouped by city.
 * Receives pre-filtered favorites from ACTimeline (already excludes reserved activities).
 */
class ACSavedPlans : BaseActivity<AcSavedPlansBinding, ACSavedPlansVM>() {

    private var adapter: AdapterSavedPlans? = null
    private var timeSelectionBottomSheet: ActivityTimeSelectionBottomSheet? = null
    private var pendingAddedName: String? = null
    private var pendingAddedDate: Date? = null

    override fun getViewBinding() = AcSavedPlansBinding.inflate(layoutInflater)

    override fun setListeners() {
        setupRecyclerView()
        setupClickListeners()

        intent?.let { intent ->
            @Suppress("DEPRECATION")
            val favorites = intent.getParcelableArrayListExtra<SegmentFavoriteItem>(EXTRA_FAVORITES)
            val tripHash = intent.getStringExtra(EXTRA_TRIP_HASH) ?: ""
            val availableDaysLong = intent.getLongArrayExtra(EXTRA_AVAILABLE_DAYS)

            val availableDays = availableDaysLong?.map { Date(it) } ?: emptyList()

            @Suppress("UNCHECKED_CAST")
            val cityMap = intent.getSerializableExtra(EXTRA_CITY_MAP) as? HashMap<String, Int> ?: hashMapOf()

            val plannedActivityIds =
                intent.getSerializableExtra(EXTRA_PLANNED_ACTIVITY_IDS).asIdsByDay()

            viewModel.initialize(
                favorites ?: emptyList(),
                tripHash,
                availableDays,
                cityMap,
                plannedActivityIds
            )
        }
    }

    override fun setReceivers() {
        binding.tvTitle.text = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_SAVED_PLANS)

        binding.tvAllSetTitle.text = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_ALL_ADDED_TITLE)
        binding.tvAllSetDescription.text = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_ALL_ADDED_DESCRIPTION)
        binding.btnViewItinerary.text = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_VIEW_ITINERARY)

        viewModel.listItems.observe(this) { items ->
            adapter?.submitList(items)
            val isEmpty = items.isEmpty()
            binding.emptyStateContainer.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.rvSavedPlans.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }

        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) showLoading() else hideLoading()
        }

        viewModel.showTimeSelection.observe(this) { favorite ->
            favorite?.let { showTimeSelectionBottomSheet(it) }
        }

        viewModel.segmentCreated.observe(this) { created ->
            if (created) {
                viewModel.resetSegmentCreated()
                timeSelectionBottomSheet?.dismiss()
                showAddedToItinerarySuccess()
                setResult(RESULT_OK)
            }
        }

        viewModel.segmentCreationFailed.observe(this) { failed ->
            failed?.let {
                viewModel.resetSegmentCreationFailed()
                timeSelectionBottomSheet?.hideInSheetLoadingOverlay()
            }
        }

        viewModel.favoriteRemoved.observe(this) { removed ->
            removed?.let {
                viewModel.resetFavoriteRemoved()
                timeSelectionBottomSheet?.dismiss()
                setResult(RESULT_OK)
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = AdapterSavedPlans(
            getLanguage = { key -> viewModel.getLanguageForKey(key) },
            onAddClicked = { favorite -> viewModel.onActivityAddClicked(favorite) },
            onItemClicked = { favorite ->
                favorite.activityId?.let { activityId ->
                    TRPCore.notifyActivityDetailRequested(activityId)
                }
            }
        )

        binding.rvSavedPlans.apply {
            layoutManager = LinearLayoutManager(this@ACSavedPlans)
            adapter = this@ACSavedPlans.adapter
        }
    }

    private fun setupClickListeners() {
        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.btnViewItinerary.setOnClickListener {
            onViewItineraryTapped()
        }
    }

    /**
     * Handles the empty-state "View itinerary" action: flags the result so the
     * timeline refreshes and closes this screen to return to it.
     */
    private fun onViewItineraryTapped() {
        setResult(RESULT_OK)
        finish()
    }

    /**
     * Shows the time selection bottom sheet for a favorite.
     * The bottom sheet handles schedule loading via its own ViewModel.
     */
    private fun showTimeSelectionBottomSheet(favorite: SegmentFavoriteItem) {
        viewModel.clearTimeSelectionTrigger()

        val resolvedCityId = favorite.cityId?.takeIf { it > 0 }
            ?: viewModel.getResolvedCityId(favorite.cityName)

        timeSelectionBottomSheet = ActivityTimeSelectionBottomSheet.newInstanceForFavorite(
            favoriteActivityId = favorite.activityId,
            favoriteCityId = resolvedCityId,
            favoriteTitle = favorite.title,
            favoriteDuration = favorite.duration,
            availableDays = viewModel.getAvailableDays(),
            initialSelectedDay = viewModel.getSelectedDate(),
            showSelectAndRemove = true,
            plannedActivityIdsByDay = viewModel.getPlannedActivityIdsByDay()
        )

        timeSelectionBottomSheet?.setOnFavoriteTimeSelectedListener { selectedDate, startTime, endTime, isFlexible, slotPrice ->
            pendingAddedName = favorite.title
            pendingAddedDate = selectedDate
            timeSelectionBottomSheet?.showInSheetLoadingOverlay(
                LanguageConst.LOADING_TEXT_ADDING_TO_ITINERARY, "Adding to itinerary"
            )
            viewModel.createReservedActivitySegment(selectedDate, startTime, endTime, isFlexible, slotPrice)
        }

        timeSelectionBottomSheet?.setOnRemoveListener {
            showRemoveConfirmation(favorite)
        }

        timeSelectionBottomSheet?.show(supportFragmentManager, ActivityTimeSelectionBottomSheet.TAG)
    }

    private fun showAddedToItinerarySuccess() {
        val name = pendingAddedName ?: return
        val date = pendingAddedDate ?: return
        val dayLabel = SimpleDateFormat("EEEE dd/MM", Locale.getDefault()).format(date)
        val message = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_TOAST_ACTIVITY_ADDED)
            .replace("%1\$@", name)
            .replace("%2\$@", dayLabel)
        showAlert(AlertType.SUCCESS, message)
    }

    /**
     * Shows the confirmation alert before removing a favorite from saved plans.
     * On confirm the removal is performed by the ViewModel.
     */
    private fun showRemoveConfirmation(favorite: SegmentFavoriteItem) {
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

    companion object {
        const val EXTRA_FAVORITES = "extra_favorites"
        const val EXTRA_TRIP_HASH = "extra_trip_hash"
        const val EXTRA_AVAILABLE_DAYS = "extra_available_days"
        const val EXTRA_CITY_MAP = "extra_city_map"
        const val EXTRA_PLANNED_ACTIVITY_IDS = "extra_planned_activity_ids"

        /**
         * Launch SavedPlans screen with pre-filtered favorites
         * @param favorites List of favorites that haven't been added as reserved_activity yet
         * @param cityNameToIdMap Mapping of cityName (lowercase) to our system's cityId
         * @param plannedActivityIdsByDay "yyyy-MM-dd" → activity ids that day already holds
         */
        fun launch(
            context: Context,
            favorites: List<SegmentFavoriteItem>,
            tripHash: String,
            availableDays: List<Date>,
            cityNameToIdMap: Map<String, Int> = emptyMap(),
            plannedActivityIdsByDay: Map<String, List<String>> = emptyMap()
        ): Intent {
            return Intent(context, ACSavedPlans::class.java).apply {
                putParcelableArrayListExtra(EXTRA_FAVORITES, ArrayList(favorites))
                putExtra(EXTRA_TRIP_HASH, tripHash)
                putExtra(EXTRA_AVAILABLE_DAYS, availableDays.map { it.time }.toLongArray())
                putExtra(EXTRA_CITY_MAP, HashMap(cityNameToIdMap))
                putExtra(
                    EXTRA_PLANNED_ACTIVITY_IDS,
                    plannedActivityIdsByDay.toSerializableIdsByDay()
                )
            }
        }
    }
}
