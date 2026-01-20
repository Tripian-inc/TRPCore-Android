package com.tripian.trpcore.ui.timeline.savedplans

import android.content.Context
import android.content.Intent
import androidx.recyclerview.widget.LinearLayoutManager
import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.databinding.AcSavedPlansBinding
import com.tripian.trpcore.domain.model.itinerary.SegmentFavoriteItem
import com.tripian.trpcore.ui.timeline.activity.ActivityTimeSelectionBottomSheet
import com.tripian.trpcore.util.LanguageConst
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

            // Initialize ViewModel with filtered favorites
            viewModel.initialize(favorites ?: emptyList(), tripHash, availableDays)
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
    }

    private fun setupRecyclerView() {
        adapter = AdapterSavedPlans(
            getLanguage = { key -> viewModel.getLanguageForKey(key) },
            onAddClicked = { favorite -> viewModel.onActivityAddClicked(favorite) }
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

        timeSelectionBottomSheet = ActivityTimeSelectionBottomSheet.newInstanceForFavorite(
            favoriteActivityId = favorite.activityId,
            favoriteCityId = favorite.cityId,
            favoriteTitle = favorite.title,
            favoriteDuration = favorite.duration,
            availableDays = viewModel.getAvailableDays(),
            initialSelectedDay = viewModel.getSelectedDate()
        )

        timeSelectionBottomSheet?.setOnFavoriteTimeSelectedListener { selectedDate, startTime, _ ->
            // Create reserved activity segment with selected date and time
            viewModel.createReservedActivitySegment(selectedDate, startTime)
        }

        timeSelectionBottomSheet?.show(supportFragmentManager, ActivityTimeSelectionBottomSheet.TAG)
    }

    companion object {
        const val EXTRA_FAVORITES = "extra_favorites"
        const val EXTRA_TRIP_HASH = "extra_trip_hash"
        const val EXTRA_AVAILABLE_DAYS = "extra_available_days"

        /**
         * Launch SavedPlans screen with pre-filtered favorites
         * @param favorites List of favorites that haven't been added as reserved_activity yet
         */
        fun launch(
            context: Context,
            favorites: List<SegmentFavoriteItem>,
            tripHash: String,
            availableDays: List<Date>
        ): Intent {
            return Intent(context, ACSavedPlans::class.java).apply {
                putParcelableArrayListExtra(EXTRA_FAVORITES, ArrayList(favorites))
                putExtra(EXTRA_TRIP_HASH, tripHash)
                putExtra(EXTRA_AVAILABLE_DAYS, availableDays.map { it.time }.toLongArray())
            }
        }
    }
}
