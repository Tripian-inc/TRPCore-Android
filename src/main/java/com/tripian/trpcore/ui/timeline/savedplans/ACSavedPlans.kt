package com.tripian.trpcore.ui.timeline.savedplans

import android.content.Context
import android.content.Intent
import androidx.recyclerview.widget.LinearLayoutManager
import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.AcSavedPlansBinding
import com.tripian.trpcore.domain.model.itinerary.SegmentFavoriteItem
import com.tripian.trpcore.ui.timeline.activity.ActivityTimeSelectionBottomSheet
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.dialog.DGActionListener
import java.util.Date

/**
 * ACSavedPlans
 * Full screen Activity for displaying saved plans (favorites) grouped by city
 * Similar flow to ACActivityListing
 *
 * Note: Receives pre-filtered favorites from ACTimeline (already excludes reserved activities)
 */
class ACSavedPlans : BaseActivity<AcSavedPlansBinding, ACSavedPlansVM>() {

    private var adapter: AdapterSavedPlans? = null
    private var timeSelectionBottomSheet: ActivityTimeSelectionBottomSheet? = null

    override fun getViewBinding() = AcSavedPlansBinding.inflate(layoutInflater)

    override fun setListeners() {
        setupRecyclerView()
        setupClickListeners()

        // Initialize ViewModel with data from intent
        intent?.let { intent ->
            @Suppress("DEPRECATION")
            val favorites = intent.getParcelableArrayListExtra<SegmentFavoriteItem>(EXTRA_FAVORITES)
            val tripHash = intent.getStringExtra(EXTRA_TRIP_HASH) ?: ""
            val availableDaysLong = intent.getLongArrayExtra(EXTRA_AVAILABLE_DAYS)

            // Convert long array back to Date list
            val availableDays = availableDaysLong?.map { Date(it) } ?: emptyList()

            // Get city name to ID mapping from intent
            @Suppress("UNCHECKED_CAST")
            val cityMap = intent.getSerializableExtra(EXTRA_CITY_MAP) as? HashMap<String, Int> ?: hashMapOf()

            // Initialize ViewModel with filtered favorites and city mapping
            viewModel.initialize(favorites ?: emptyList(), tripHash, availableDays, cityMap)
        }
    }

    override fun setReceivers() {
        // Set title
        binding.tvTitle.text = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_SAVED_PLANS)

        // Observe list items
        viewModel.listItems.observe(this) { items ->
            adapter?.submitList(items)
        }

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) showLoading() else hideLoading()
        }

        // Observe time selection trigger
        viewModel.showTimeSelection.observe(this) { favorite ->
            favorite?.let { showTimeSelectionBottomSheet(it) }
        }

        // Observe segment creation loading state
        viewModel.isCreatingSegment.observe(this) { isCreating ->
            if (isCreating) {
                // Dismiss bottom sheet first
                timeSelectionBottomSheet?.dismiss()
                // Show loading after bottom sheet dismiss animation completes
                binding.root.postDelayed({
                    showLoading()
                }, 300)
            } else {
                hideLoading()
            }
        }

        // Observe segment created
        viewModel.segmentCreated.observe(this) { created ->
            if (created) {
                viewModel.resetSegmentCreated()
                // Return success result - timeline will refresh
                setResult(RESULT_OK)
                finish()
            }
        }

        // Observe favorite removed - dismiss the sheet and refresh the timeline.
        // Close the screen entirely once the saved plans list is empty.
        viewModel.favoriteRemoved.observe(this) { listEmpty ->
            listEmpty?.let {
                viewModel.resetFavoriteRemoved()
                timeSelectionBottomSheet?.dismiss()
                // Timeline refreshes its saved-plans badge on RESULT_OK.
                setResult(RESULT_OK)
                if (it) finish()
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = AdapterSavedPlans(
            getLanguage = { key -> viewModel.getLanguageForKey(key) },
            onAddClicked = { favorite -> viewModel.onActivityAddClicked(favorite) },
            onItemClicked = { favorite ->
                // Notify host app that user tapped on an activity to see details
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
    }

    /**
     * Show time selection bottom sheet for favorite
     * Uses the same bottom sheet as ACActivityListing with schedule API loading
     * Note: The bottom sheet handles schedule loading via its own ViewModel
     */
    private fun showTimeSelectionBottomSheet(favorite: SegmentFavoriteItem) {
        viewModel.clearTimeSelectionTrigger()

        // Get resolved cityId from mapping (our system's ID) instead of host app's cityId
        val resolvedCityId = viewModel.getResolvedCityId(favorite.cityName)

        timeSelectionBottomSheet = ActivityTimeSelectionBottomSheet.newInstanceForFavorite(
            favoriteActivityId = favorite.activityId,
            favoriteCityId = resolvedCityId ?: favorite.cityId,  // Use resolved cityId if available
            favoriteTitle = favorite.title,
            favoriteDuration = favorite.duration,
            availableDays = viewModel.getAvailableDays(),
            initialSelectedDay = viewModel.getSelectedDate(),
            // SavedPlans flow: "Select" primary + outlined "Remove".
            showSelectAndRemove = true
        )

        timeSelectionBottomSheet?.setOnFavoriteTimeSelectedListener { selectedDate, startTime, endTime, isFlexible, slotPrice ->
            // Create reserved activity segment with selected date, time and slot price
            viewModel.createReservedActivitySegment(selectedDate, startTime, endTime, isFlexible, slotPrice)
        }

        timeSelectionBottomSheet?.setOnRemoveListener {
            showRemoveConfirmation(favorite)
        }

        timeSelectionBottomSheet?.show(supportFragmentManager, ActivityTimeSelectionBottomSheet.TAG)
    }

    /**
     * Shows the confirmation alert before removing a favorite from saved plans.
     * On confirm the removal is performed by the ViewModel.
     */
    private fun showRemoveConfirmation(favorite: SegmentFavoriteItem) {
        viewModel.showDialog(
            title = viewModel.getLanguageForKey(LanguageConst.REMOVE_ACTIVITY),
            contentText = viewModel.getLanguageForKey(LanguageConst.SAVED_PLANS_REMOVE_CONFIRM)
                .ifBlank { "Are you sure you want to remove this activity from your saved plans?" },
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

        /**
         * Launch SavedPlans screen with pre-filtered favorites
         * @param favorites List of favorites that haven't been added as reserved_activity yet
         * @param cityNameToIdMap Mapping of cityName (lowercase) to our system's cityId
         */
        fun launch(
            context: Context,
            favorites: List<SegmentFavoriteItem>,
            tripHash: String,
            availableDays: List<Date>,
            cityNameToIdMap: Map<String, Int> = emptyMap()
        ): Intent {
            return Intent(context, ACSavedPlans::class.java).apply {
                putParcelableArrayListExtra(EXTRA_FAVORITES, ArrayList(favorites))
                putExtra(EXTRA_TRIP_HASH, tripHash)
                putExtra(EXTRA_AVAILABLE_DAYS, availableDays.map { it.time }.toLongArray())
                putExtra(EXTRA_CITY_MAP, HashMap(cityNameToIdMap))
            }
        }
    }
}
