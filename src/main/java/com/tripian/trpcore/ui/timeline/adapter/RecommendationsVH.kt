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

    private var stepsAdapter: TimelineStepsAdapter? = null
    private var currentStepClickListener: ((TimelineStep) -> Unit)? = null
    private var currentChangeTimeClickListener: ((TimelineStep) -> Unit)? = null
    private var currentDeleteClickListener: ((TimelineStep) -> Unit)? = null
    private var currentReservationClickListener: ((TimelineStep) -> Unit)? = null
    private var currentStartingOrder: Int = 1

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

        // Title only - no city, time, places info
        binding.tvTitle.text = item.title

        // Generation status
        if (item.isGenerating) {
            binding.progressGenerating.visibility = View.VISIBLE
            binding.tvGenerating.visibility = View.VISIBLE
            binding.rvSteps.visibility = View.GONE
            binding.tvNoRecommendations.visibility = View.GONE
            binding.startingPointContainer.visibility = View.GONE
            binding.startingPointRouteContainer.visibility = View.GONE
        } else if (item.hasNoPois) {
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

            // Starting Point - show if expanded and has starting point name
            val startingPointName = item.startingPointName
            if (item.isExpanded && !startingPointName.isNullOrEmpty()) {
                binding.tvStartingPointName.text = startingPointName
                binding.startingPointContainer.visibility = View.VISIBLE

                // Dynamically adjust corner radius to be half of container height
                binding.startingPointContainer.post {
                    updateStartingPointBackground()
                }
            } else {
                binding.startingPointContainer.visibility = View.GONE
            }

            // Starting Point Route - route from starting point to first step
            // This route has fromStepId = null
            val startingPointRoute = item.routeInfoList.find { it.fromStepId == null }
            if (item.isExpanded && startingPointRoute != null && item.steps.isNotEmpty()) {
                binding.startingPointRouteContainer.visibility = View.VISIBLE
                binding.tvStartingPointRouteInfo.text = startingPointRoute.formatWithTemplate(distanceFormat)
            } else {
                binding.startingPointRouteContainer.visibility = View.GONE
            }

            // Steps
            if (item.steps.isNotEmpty() && item.isExpanded) {
                binding.rvSteps.visibility = View.VISIBLE
                setupStepsAdapter()

                // Build step items with route separators if available
                // Exclude the starting point route (fromStepId = null) as it's shown separately
                val stepRoutes = item.routeInfoList.filter { it.fromStepId != null }
                val stepItems = buildStepItemsWithRoutes(item.steps, stepRoutes)
                stepsAdapter?.submitStepItemList(stepItems)

                // Request route calculation if route info is empty and we have steps
                // Use post() to defer the callback to avoid calling submitList() during layout
                if (item.routeInfoList.isEmpty() && item.steps.size > 0) {
                    binding.root.post {
                        onRequestRouteCalculation?.invoke(item)
                    }
                }
            } else {
                binding.rvSteps.visibility = View.GONE
            }
        }

        // Expand/Collapse icon - chevron up when expanded, down when collapsed
        binding.ivExpand.setImageResource(
            if (item.isExpanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down
        )

        // Click listeners
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

    private fun setupStepsAdapter() {
        // Always recreate adapter to ensure all callbacks and startingOrder are properly set
        stepsAdapter = TimelineStepsAdapter(
            startingOrder = currentStartingOrder,
            onStepClick = currentStepClickListener,
            onChangeTimeClick = currentChangeTimeClickListener,
            onDeleteClick = currentDeleteClickListener,
            onReservationClick = currentReservationClickListener
        )
        binding.rvSteps.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = stepsAdapter
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
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
            val strokeColor = ContextCompat.getColor(container.context, R.color.lineWeak)

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
     * Builds an interleaved list of TimelineStepItem containing:
     * - Route separators between steps (if route info available)
     * - Step items with correct order numbers
     *
     * Result order:
     * [RouteSeparator(start→step1), Step(1), RouteSeparator(step1→step2), Step(2), ...]
     *
     * @param steps List of TimelineStep to display
     * @param routeInfoList List of StepRouteInfo for route separators
     * @return Interleaved list of TimelineStepItem
     */
    private fun buildStepItemsWithRoutes(
        steps: List<TimelineStep>,
        routeInfoList: List<StepRouteInfo>
    ): List<TimelineStepItem> {
        val items = mutableListOf<TimelineStepItem>()

        // Create a map of route info by toStepId for quick lookup
        val routeInfoMap = routeInfoList.associateBy { it.toStepId }

        steps.forEachIndexed { index, step ->
            val stepId = step.id ?: 0

            // Add route separator before this step if available
            routeInfoMap[stepId]?.let { routeInfo ->
                items.add(TimelineStepItem.RouteSeparator(routeInfo))
            }

            // Add the step with correct order number
            items.add(TimelineStepItem.Step(step, currentStartingOrder + index))
        }

        return items
    }
}
