package com.tripian.trpcore.ui.timeline.adapter

import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tripian.one.api.timeline.model.TimelineStep
import com.tripian.trpcore.R
import com.tripian.trpcore.databinding.ItemTimelineRecommendationsBinding
import com.tripian.trpcore.domain.model.timeline.StepRouteInfo
import com.tripian.trpcore.domain.model.timeline.TimelineDisplayItem

/**
 * RecommendationsVH
 * Smart Recommendations - AI recommendations
 * Header shows only title - no city, time, or places info
 * Shows distance/duration between steps when route info is available
 */
class RecommendationsVH(
    private val binding: ItemTimelineRecommendationsBinding
) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        /**
         * iOS parity: the starting-point row is hidden while the walking-distance
         * row and route calculation still use the starting-point coordinate.
         */
        private const val SHOWS_STARTING_POINT = false
    }

    private var stepsAdapter: TimelineStepsAdapter? = null
    private var currentStepClickListener: ((TimelineStep) -> Unit)? = null
    private var currentChangeTimeClickListener: ((TimelineStep) -> Unit)? = null
    private var currentDeleteClickListener: ((TimelineStep) -> Unit)? = null
    private var currentReservationClickListener: ((TimelineStep) -> Unit)? = null
    private var currentStartingOrder: Int = 1

    // For partial updates (route info) without full rebind
    private var currentItem: TimelineDisplayItem.Recommendations? = null
    private var currentDistanceFormat: String = "%d min (%@ km)"

    /**
     * Binds the card. Route calculation requests are deferred with post() to
     * avoid calling submitList() during layout.
     */
    fun bind(
        item: TimelineDisplayItem.Recommendations,
        onItemClick: (TimelineDisplayItem) -> Unit,
        onDeleteClick: (TimelineDisplayItem, Int?) -> Unit,
        onExpandClick: (TimelineDisplayItem) -> Unit,
        onStepClick: ((TimelineStep) -> Unit)? = null,
        onStepChangeTimeClick: ((TimelineStep) -> Unit)? = null,
        onStepDeleteClick: ((TimelineStep) -> Unit)? = null,
        onStepReservationClick: ((TimelineStep) -> Unit)? = null,
        onRequestRouteCalculation: ((TimelineDisplayItem.Recommendations) -> Unit)? = null,
        distanceFormat: String = "%d min (%@ km)"
    ) {
        currentStepClickListener = onStepClick
        currentChangeTimeClickListener = onStepChangeTimeClick
        currentDeleteClickListener = onStepDeleteClick
        currentReservationClickListener = onStepReservationClick
        currentStartingOrder = item.startingOrder

        currentItem = item
        currentDistanceFormat = distanceFormat

        binding.tvTitle.text = item.title

        if (item.isGenerating) {
            binding.progressGenerating.visibility = View.VISIBLE
            binding.tvGenerating.visibility = View.VISIBLE
            binding.rvSteps.visibility = View.GONE
            binding.tvNoRecommendations.visibility = View.GONE
            binding.startingPointContainer.visibility = View.GONE
            binding.startingPointRouteContainer.visibility = View.GONE
        } else if (item.hasNoPois && item.steps.isEmpty()) {
            // Only show the empty state when there is genuinely nothing to render.
            // A plan can report generatedStatus == -1 yet still carry steps (e.g.
            // steps from a prior generation); in that case fall through and render
            // them instead of the "No recommendations" placeholder.
            binding.progressGenerating.visibility = View.GONE
            binding.tvGenerating.visibility = View.GONE
            binding.rvSteps.visibility = View.GONE
            binding.tvNoRecommendations.visibility = View.VISIBLE
            binding.startingPointContainer.visibility = View.GONE
            binding.startingPointRouteContainer.visibility = View.GONE
        } else {
            binding.progressGenerating.visibility = View.GONE
            binding.tvGenerating.visibility = View.GONE
            binding.tvNoRecommendations.visibility = View.GONE

            val startingPointName = item.startingPointName
            if (SHOWS_STARTING_POINT && item.isExpanded && !startingPointName.isNullOrEmpty()) {
                binding.tvStartingPointName.text = startingPointName
                binding.startingPointContainer.visibility = View.VISIBLE

                binding.startingPointContainer.post {
                    updateStartingPointBackground()
                }
            } else {
                binding.startingPointContainer.visibility = View.GONE
            }

            val startingPointRoute = item.routeInfoList.find { it.fromStepId == null }
            if (item.isExpanded && startingPointRoute != null && item.steps.isNotEmpty()) {
                binding.startingPointRouteContainer.visibility = View.VISIBLE
                binding.tvStartingPointRouteInfo.text = startingPointRoute.formatWithTemplate(distanceFormat)
            } else {
                binding.startingPointRouteContainer.visibility = View.GONE
            }

            if (item.steps.isNotEmpty() && item.isExpanded) {
                binding.rvSteps.visibility = View.VISIBLE
                setupStepsAdapter()

                val stepRoutes = item.routeInfoList.filter { it.fromStepId != null }
                val stepItems = buildStepItemsWithRoutes(
                    steps = item.steps,
                    routeInfoList = stepRoutes,
                    conflictingStepIds = item.conflictingStepIds,
                    timeOverlapStepIds = item.timeOverlapStepIds
                )
                stepsAdapter?.submitStepItemList(stepItems)

                if (item.routeInfoList.isEmpty() && item.steps.size > 0) {
                    binding.root.post {
                        onRequestRouteCalculation?.invoke(item)
                    }
                }
            } else {
                binding.rvSteps.visibility = View.GONE
            }
        }

        binding.ivExpand.setImageResource(
            if (item.isExpanded) R.drawable.trp_ic_chevron_up else R.drawable.trp_ic_chevron_down
        )

        binding.root.setOnClickListener {
            onItemClick(item)
        }

        binding.headerContainer.setOnClickListener {
            onExpandClick(item)
        }

        binding.ivExpand.setOnClickListener {
            onExpandClick(item)
        }

        binding.btnDelete.setOnClickListener {
            onDeleteClick(item, item.segmentIndex)
        }
    }

    /**
     * Reuses the steps adapter across bind() calls — swapping RecyclerView.adapter
     * on every bind detaches/recreates child views and causes day-switch flicker.
     */
    private fun setupStepsAdapter() {
        val existing = stepsAdapter
        if (existing == null) {
            val newAdapter = TimelineStepsAdapter(
                startingOrder = currentStartingOrder,
                onStepClick = currentStepClickListener,
                onChangeTimeClick = currentChangeTimeClickListener,
                onDeleteClick = currentDeleteClickListener,
                onReservationClick = currentReservationClickListener
            )
            stepsAdapter = newAdapter
            binding.rvSteps.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = newAdapter
                setHasFixedSize(false)
                isNestedScrollingEnabled = false
            }
        } else {
            existing.startingOrder = currentStartingOrder
            existing.onStepClick = currentStepClickListener
            existing.onChangeTimeClick = currentChangeTimeClickListener
            existing.onDeleteClick = currentDeleteClickListener
            existing.onReservationClick = currentReservationClickListener
        }
    }

    /**
     * Updates starting point container background with dynamic corner radius.
     * Corner radius is set to half of the container height for pill shape.
     */
    private fun updateStartingPointBackground() {
        val container = binding.startingPointContainer
        val height = container.height
        if (height > 0) {
            val cornerRadius = height / 2f
            val strokeWidth = (1 * container.context.resources.displayMetrics.density).toInt()
            val strokeColor = ContextCompat.getColor(container.context, R.color.trp_lineWeak)

            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setCornerRadius(cornerRadius)
                setStroke(strokeWidth, strokeColor)
                setColor(android.graphics.Color.TRANSPARENT)
            }
            container.background = drawable
        }
    }

    /**
     * Builds an interleaved list of [TimelineStepItem]:
     * [RouteSeparator(start→step1), Step(1), RouteSeparator(step1→step2), Step(2), ...]
     *
     * @param conflictingStepIds Step IDs with visual conflicts (ALL overlapping steps)
     * @param timeOverlapStepIds Step IDs that should show the "Time Overlap" text
     */
    private fun buildStepItemsWithRoutes(
        steps: List<TimelineStep>,
        routeInfoList: List<StepRouteInfo>,
        conflictingStepIds: Set<Int> = emptySet(),
        timeOverlapStepIds: Set<Int> = emptySet()
    ): List<TimelineStepItem> {
        val items = mutableListOf<TimelineStepItem>()

        val routeInfoMap = routeInfoList.associateBy { it.toStepId }

        steps.forEachIndexed { index, step ->
            val stepId = step.id ?: 0

            routeInfoMap[stepId]?.let { routeInfo ->
                items.add(TimelineStepItem.RouteSeparator(routeInfo))
            }

            val hasConflict = stepId in conflictingStepIds
            val showTimeOverlap = stepId in timeOverlapStepIds

            items.add(
                TimelineStepItem.Step(
                    step = step,
                    order = currentStartingOrder + index,
                    hasConflict = hasConflict,
                    showTimeOverlapText = showTimeOverlap,
                    isAvailabilityExpired = step.isAvailabilityExpired,
                    startDateTimeSnapshot = step.startDateTimes,
                    endDateTimeSnapshot = step.endDateTimes
                )
            )
        }

        return items
    }

    /**
     * Updates only route info without a full rebind, preventing flash/flicker
     * when distance calculations complete.
     */
    fun updateRouteInfo(newItem: TimelineDisplayItem.Recommendations) {
        currentItem = newItem

        val startingPointRoute = newItem.routeInfoList.find { it.fromStepId == null }
        if (newItem.isExpanded && startingPointRoute != null && newItem.steps.isNotEmpty()) {
            binding.startingPointRouteContainer.visibility = View.VISIBLE
            binding.tvStartingPointRouteInfo.text = startingPointRoute.formatWithTemplate(currentDistanceFormat)
        }

        if (newItem.steps.isNotEmpty() && newItem.isExpanded) {
            val stepRoutes = newItem.routeInfoList.filter { it.fromStepId != null }
            val stepItems = buildStepItemsWithRoutes(
                steps = newItem.steps,
                routeInfoList = stepRoutes,
                conflictingStepIds = newItem.conflictingStepIds,
                timeOverlapStepIds = newItem.timeOverlapStepIds
            )
            stepsAdapter?.submitStepItemList(stepItems)
        }
    }
}
