package com.tripian.trpcore.ui.timeline.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tripian.one.api.timeline.model.TimelineStep
import com.tripian.trpcore.databinding.ItemTimelineBookedActivityBinding
import com.tripian.trpcore.databinding.ItemTimelineEmptyStateBinding
import com.tripian.trpcore.databinding.ItemTimelineFlexibleActivityBinding
import com.tripian.trpcore.databinding.ItemTimelineManualPoiBinding
import com.tripian.trpcore.databinding.ItemTimelineRecommendationsBinding
import com.tripian.trpcore.databinding.ItemTimelineReservedActivityBinding
import com.tripian.trpcore.databinding.ItemTimelineSectionFooterBinding
import com.tripian.trpcore.databinding.ItemTimelineSectionHeaderBinding
import com.tripian.trpcore.databinding.ItemTimelineStartingPointBinding
import com.tripian.trpcore.databinding.ItemTimelineStepActivityBinding
import com.tripian.trpcore.databinding.ItemTimelineStepPoiBinding
import com.tripian.trpcore.databinding.ItemTimelineStepRouteSeparatorBinding
import com.tripian.trpcore.domain.model.timeline.TimelineDisplayItem
import com.tripian.trpcore.ui.timeline.views.ConflictWarningView

/**
 * TimelineAdapter
 * Shows items on the Timeline screen. Flat-timeline rows (starting point, plan
 * steps, route separators) reuse the step view holders of the recommendations
 * card with a uniform 20dp vertical rhythm.
 */
class TimelineAdapter(
    private val onItemClick: (TimelineDisplayItem) -> Unit,
    private val onDeleteClick: (TimelineDisplayItem, Int?) -> Unit,
    private val onExpandClick: (TimelineDisplayItem) -> Unit,
    private val onStepClick: ((TimelineStep) -> Unit)? = null,
    private val onChangeTimeClick: ((TimelineDisplayItem.ManualPoi) -> Unit)? = null,
    private val onReservedActivityChangeTimeClick: ((TimelineDisplayItem.BookedActivity) -> Unit)? = null,
    private val onFlexibleActivityChangeTimeClick: ((TimelineDisplayItem.FlexibleActivity) -> Unit)? = null,
    private val onReservationClick: ((TimelineDisplayItem.BookedActivity) -> Unit)? = null,
    private val onFlexibleReservationClick: ((TimelineDisplayItem.FlexibleActivity) -> Unit)? = null,
    private val onAddPlanClick: (() -> Unit)? = null,
    // Step callbacks for Recommendations
    private val onStepChangeTimeClick: ((TimelineStep) -> Unit)? = null,
    private val onStepDeleteClick: ((TimelineStep) -> Unit)? = null,
    private val onStepReservationClick: ((TimelineStep) -> Unit)? = null,
    private val onRequestRouteCalculation: ((TimelineDisplayItem.Recommendations) -> Unit)? = null,
    private val onSectionToggle: ((cityId: Int) -> Unit)? = null,
    private val isSectionCollapsed: ((cityId: Int) -> Boolean)? = null,
    private val onConflictTap: (() -> Unit)? = null,
    private val onConflictDismiss: (() -> Unit)? = null
) : ListAdapter<TimelineDisplayItem, RecyclerView.ViewHolder>(TimelineDiffCallback()) {

    companion object {
        private const val TYPE_SECTION_HEADER = 0
        private const val TYPE_BOOKED_ACTIVITY = 1
        private const val TYPE_RECOMMENDATIONS = 2
        private const val TYPE_MANUAL_POI = 3
        private const val TYPE_EMPTY_STATE = 4
        private const val TYPE_SECTION_FOOTER = 6
        private const val TYPE_RESERVED_ACTIVITY = 7
        private const val TYPE_FLEXIBLE_ACTIVITY = 8
        private const val TYPE_CONFLICT_WARNING = 9
        private const val TYPE_STARTING_POINT = 10
        private const val TYPE_PLAN_STEP_POI = 11
        private const val TYPE_PLAN_STEP_ACTIVITY = 12
        private const val TYPE_ROUTE_SEPARATOR = 13

        private const val FLAT_ROW_SPACING_DP = 20

        const val PAYLOAD_ROUTE_INFO_UPDATE = "route_info_update"
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = getItem(position)) {
            is TimelineDisplayItem.SectionHeader -> TYPE_SECTION_HEADER
            is TimelineDisplayItem.BookedActivity -> {
                if (item.isReserved) TYPE_RESERVED_ACTIVITY else TYPE_BOOKED_ACTIVITY
            }
            is TimelineDisplayItem.FlexibleActivity -> TYPE_FLEXIBLE_ACTIVITY
            is TimelineDisplayItem.Recommendations -> TYPE_RECOMMENDATIONS
            is TimelineDisplayItem.ManualPoi -> TYPE_MANUAL_POI
            is TimelineDisplayItem.EmptyState -> TYPE_EMPTY_STATE
            is TimelineDisplayItem.SectionFooter -> TYPE_SECTION_FOOTER
            is TimelineDisplayItem.ConflictWarning -> TYPE_CONFLICT_WARNING
            is TimelineDisplayItem.StartingPoint -> TYPE_STARTING_POINT
            is TimelineDisplayItem.PlanStep -> {
                if (item.isActivity) TYPE_PLAN_STEP_ACTIVITY else TYPE_PLAN_STEP_POI
            }
            is TimelineDisplayItem.RouteSeparator -> TYPE_ROUTE_SEPARATOR
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_SECTION_HEADER -> SectionHeaderVH(
                ItemTimelineSectionHeaderBinding.inflate(inflater, parent, false)
            )
            TYPE_BOOKED_ACTIVITY -> BookedActivityVH(
                ItemTimelineBookedActivityBinding.inflate(inflater, parent, false)
            )
            TYPE_RESERVED_ACTIVITY -> ReservedActivityVH(
                ItemTimelineReservedActivityBinding.inflate(inflater, parent, false)
            )
            TYPE_FLEXIBLE_ACTIVITY -> FlexibleActivityVH(
                ItemTimelineFlexibleActivityBinding.inflate(inflater, parent, false)
            )
            TYPE_RECOMMENDATIONS -> RecommendationsVH(
                ItemTimelineRecommendationsBinding.inflate(inflater, parent, false)
            )
            TYPE_MANUAL_POI -> ManualPoiVH(
                ItemTimelineManualPoiBinding.inflate(inflater, parent, false)
            )
            TYPE_EMPTY_STATE -> EmptyStateVH(
                ItemTimelineEmptyStateBinding.inflate(inflater, parent, false)
            )
            TYPE_SECTION_FOOTER -> SectionFooterVH(
                ItemTimelineSectionFooterBinding.inflate(inflater, parent, false)
            )
            TYPE_CONFLICT_WARNING -> {
                val view = inflater.inflate(
                    com.tripian.trpcore.R.layout.item_timeline_conflict_warning,
                    parent,
                    false
                ) as ConflictWarningView
                ConflictWarningVH(view)
            }
            TYPE_STARTING_POINT -> StartingPointVH(
                ItemTimelineStartingPointBinding.inflate(inflater, parent, false)
            )
            TYPE_PLAN_STEP_POI -> StepPoiVH(
                ItemTimelineStepPoiBinding.inflate(inflater, parent, false)
                    .also { it.root.styleAsFlatRow() }
            )
            TYPE_PLAN_STEP_ACTIVITY -> StepActivityVH(
                ItemTimelineStepActivityBinding.inflate(inflater, parent, false)
                    .also { it.root.styleAsFlatRow() }
            )
            TYPE_ROUTE_SEPARATOR -> StepRouteSeparatorVH(
                ItemTimelineStepRouteSeparatorBinding.inflate(inflater, parent, false)
                    .also { it.root.styleAsFlatSeparator() }
            )
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    /** Card step layouts sit on the card's grey ground; at top level they go on white with bottom spacing. */
    private fun View.styleAsFlatRow() {
        background = null
        val bottom = (FLAT_ROW_SPACING_DP * resources.displayMetrics.density).toInt()
        setPadding(paddingLeft, paddingTop, paddingRight, bottom)
    }

    private fun View.styleAsFlatSeparator() {
        background = null
        (layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin = 0
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty() && payloads[0] == PAYLOAD_ROUTE_INFO_UPDATE) {
            val item = getItem(position)
            if (holder is RecommendationsVH && item is TimelineDisplayItem.Recommendations) {
                holder.updateRouteInfo(item)
                return
            }
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is SectionHeaderVH -> {
                val header = item as TimelineDisplayItem.SectionHeader
                val cityId = header.city?.id ?: 0
                val collapsed = isSectionCollapsed?.invoke(cityId) ?: false
                holder.bind(header, collapsed, onSectionToggle)
            }
            is BookedActivityVH -> holder.bind(
                item as TimelineDisplayItem.BookedActivity,
                onItemClick,
                onDeleteClick
            )
            is ReservedActivityVH -> holder.bind(
                item as TimelineDisplayItem.BookedActivity,
                onItemClick,
                onReservedActivityChangeTimeClick ?: {},
                onDeleteClick,
                onReservationClick ?: {}
            )
            is FlexibleActivityVH -> holder.bind(
                item as TimelineDisplayItem.FlexibleActivity,
                onItemClick,
                onFlexibleActivityChangeTimeClick ?: {},
                onDeleteClick,
                onFlexibleReservationClick ?: {}
            )
            is RecommendationsVH -> holder.bind(
                item = item as TimelineDisplayItem.Recommendations,
                onItemClick = onItemClick,
                onDeleteClick = onDeleteClick,
                onExpandClick = onExpandClick,
                onStepClick = onStepClick,
                onStepChangeTimeClick = onStepChangeTimeClick,
                onStepDeleteClick = onStepDeleteClick,
                onStepReservationClick = onStepReservationClick,
                onRequestRouteCalculation = onRequestRouteCalculation
            )
            is ManualPoiVH -> holder.bind(
                item as TimelineDisplayItem.ManualPoi,
                onItemClick,
                onChangeTimeClick,
                onDeleteClick
            )
            is EmptyStateVH -> holder.bind(item as TimelineDisplayItem.EmptyState, onAddPlanClick)
            is SectionFooterVH -> { }
            is ConflictWarningVH -> holder.bind(
                onTap = onConflictTap ?: {},
                onDismiss = onConflictDismiss ?: {}
            )
            is StartingPointVH -> holder.bind(item as TimelineDisplayItem.StartingPoint)
            is StepPoiVH -> {
                val planStep = item as TimelineDisplayItem.PlanStep
                holder.bind(
                    step = planStep.step,
                    order = planStep.order,
                    onStepClick = onStepClick,
                    onChangeTimeClick = onStepChangeTimeClick,
                    onDeleteClick = onStepDeleteClick,
                    hasConflict = planStep.hasConflict,
                    showTimeOverlapText = planStep.showTimeOverlapText
                )
            }
            is StepActivityVH -> {
                val planStep = item as TimelineDisplayItem.PlanStep
                holder.bind(
                    step = planStep.step,
                    order = planStep.order,
                    onStepClick = onStepClick,
                    onChangeTimeClick = onStepChangeTimeClick,
                    onDeleteClick = onStepDeleteClick,
                    onReservationClick = onStepReservationClick,
                    hasConflict = planStep.hasConflict,
                    showTimeOverlapText = planStep.showTimeOverlapText,
                    isAvailabilityExpired = planStep.isAvailabilityExpired
                )
            }
            is StepRouteSeparatorVH -> holder.bind((item as TimelineDisplayItem.RouteSeparator).routeInfo)
        }
    }
}

/**
 * DiffCallback for TimelineDisplayItem
 */
class TimelineDiffCallback : DiffUtil.ItemCallback<TimelineDisplayItem>() {
    override fun areItemsTheSame(
        oldItem: TimelineDisplayItem,
        newItem: TimelineDisplayItem
    ): Boolean {
        return when {
            oldItem is TimelineDisplayItem.SectionHeader && newItem is TimelineDisplayItem.SectionHeader ->
                oldItem.cityName == newItem.cityName
            oldItem is TimelineDisplayItem.BookedActivity && newItem is TimelineDisplayItem.BookedActivity ->
                oldItem.segment.title == newItem.segment.title && oldItem.segment.startDate == newItem.segment.startDate
            oldItem is TimelineDisplayItem.FlexibleActivity && newItem is TimelineDisplayItem.FlexibleActivity ->
                oldItem.segment.title == newItem.segment.title && oldItem.segment.startDate == newItem.segment.startDate
            oldItem is TimelineDisplayItem.Recommendations && newItem is TimelineDisplayItem.Recommendations ->
                oldItem.plan.id == newItem.plan.id
            oldItem is TimelineDisplayItem.ManualPoi && newItem is TimelineDisplayItem.ManualPoi ->
                oldItem.step.id == newItem.step.id
            oldItem is TimelineDisplayItem.EmptyState && newItem is TimelineDisplayItem.EmptyState ->
                true
            oldItem is TimelineDisplayItem.SectionFooter && newItem is TimelineDisplayItem.SectionFooter ->
                oldItem.city?.id == newItem.city?.id
            oldItem is TimelineDisplayItem.ConflictWarning && newItem is TimelineDisplayItem.ConflictWarning ->
                true
            oldItem is TimelineDisplayItem.StartingPoint && newItem is TimelineDisplayItem.StartingPoint ->
                oldItem.city?.id == newItem.city?.id
            oldItem is TimelineDisplayItem.PlanStep && newItem is TimelineDisplayItem.PlanStep ->
                oldItem.step.id == newItem.step.id
            oldItem is TimelineDisplayItem.RouteSeparator && newItem is TimelineDisplayItem.RouteSeparator ->
                oldItem.city?.id == newItem.city?.id &&
                        oldItem.routeInfo.fromStepId == newItem.routeInfo.fromStepId &&
                        oldItem.routeInfo.toStepId == newItem.routeInfo.toStepId
            else -> false
        }
    }

    override fun areContentsTheSame(
        oldItem: TimelineDisplayItem,
        newItem: TimelineDisplayItem
    ): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(
        oldItem: TimelineDisplayItem,
        newItem: TimelineDisplayItem
    ): Any? {
        if (oldItem is TimelineDisplayItem.Recommendations &&
            newItem is TimelineDisplayItem.Recommendations) {
            if (oldItem.plan.id == newItem.plan.id &&
                oldItem.routeInfoList != newItem.routeInfoList &&
                oldItem.copy(routeInfoList = newItem.routeInfoList) == newItem) {
                return TimelineAdapter.PAYLOAD_ROUTE_INFO_UPDATE
            }
        }
        return null
    }
}
