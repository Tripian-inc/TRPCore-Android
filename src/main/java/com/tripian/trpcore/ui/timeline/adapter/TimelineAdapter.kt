package com.tripian.trpcore.ui.timeline.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.databinding.ItemTimelineBookedActivityBinding
import com.tripian.trpcore.databinding.ItemTimelineEmptyStateBinding
import com.tripian.trpcore.databinding.ItemTimelineGeneratingBinding
import com.tripian.trpcore.databinding.ItemTimelineManualPoiBinding
import com.tripian.trpcore.databinding.ItemTimelineRecommendationsBinding
import com.tripian.trpcore.databinding.ItemTimelineReservedActivityBinding
import com.tripian.trpcore.databinding.ItemTimelineSectionFooterBinding
import com.tripian.trpcore.databinding.ItemTimelineSectionHeaderBinding
import com.tripian.trpcore.domain.model.timeline.TimelineDisplayItem

/**
 * TimelineAdapter
 * Shows items on the Timeline screen
 */
class TimelineAdapter(
    private val onItemClick: (TimelineDisplayItem) -> Unit,
    private val onDeleteClick: (TimelineDisplayItem, Int?) -> Unit,
    private val onExpandClick: (TimelineDisplayItem) -> Unit,
    private val onStepClick: ((com.tripian.one.api.timeline.model.TimelineStep) -> Unit)? = null,
    private val onChangeTimeClick: ((TimelineDisplayItem.ManualPoi) -> Unit)? = null,
    private val onReservationClick: ((TimelineDisplayItem.BookedActivity) -> Unit)? = null,
    private val onAddPlanClick: (() -> Unit)? = null,
    // Step callbacks for Recommendations
    private val onStepChangeTimeClick: ((com.tripian.one.api.timeline.model.TimelineStep) -> Unit)? = null,
    private val onStepDeleteClick: ((com.tripian.one.api.timeline.model.TimelineStep) -> Unit)? = null,
    private val onStepReservationClick: ((com.tripian.one.api.timeline.model.TimelineStep) -> Unit)? = null,
    // Route calculation callback for Recommendations
    private val onRequestRouteCalculation: ((TimelineDisplayItem.Recommendations) -> Unit)? = null
) : ListAdapter<TimelineDisplayItem, RecyclerView.ViewHolder>(TimelineDiffCallback()) {

    companion object {
        private const val TYPE_SECTION_HEADER = 0
        private const val TYPE_BOOKED_ACTIVITY = 1
        private const val TYPE_RECOMMENDATIONS = 2
        private const val TYPE_MANUAL_POI = 3
        private const val TYPE_EMPTY_STATE = 4
        private const val TYPE_GENERATING = 5
        private const val TYPE_SECTION_FOOTER = 6
        private const val TYPE_RESERVED_ACTIVITY = 7
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = getItem(position)) {
            is TimelineDisplayItem.SectionHeader -> TYPE_SECTION_HEADER
            is TimelineDisplayItem.BookedActivity -> {
                // Differentiate between booked and reserved activities
                if (item.isReserved) TYPE_RESERVED_ACTIVITY else TYPE_BOOKED_ACTIVITY
            }
            is TimelineDisplayItem.Recommendations -> TYPE_RECOMMENDATIONS
            is TimelineDisplayItem.ManualPoi -> TYPE_MANUAL_POI
            is TimelineDisplayItem.EmptyState -> TYPE_EMPTY_STATE
            is TimelineDisplayItem.GeneratingState -> TYPE_GENERATING
            is TimelineDisplayItem.SectionFooter -> TYPE_SECTION_FOOTER
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
            TYPE_RECOMMENDATIONS -> RecommendationsVH(
                ItemTimelineRecommendationsBinding.inflate(inflater, parent, false)
            )
            TYPE_MANUAL_POI -> ManualPoiVH(
                ItemTimelineManualPoiBinding.inflate(inflater, parent, false)
            )
            TYPE_EMPTY_STATE -> EmptyStateVH(
                ItemTimelineEmptyStateBinding.inflate(inflater, parent, false)
            )
            TYPE_GENERATING -> GeneratingStateVH(
                ItemTimelineGeneratingBinding.inflate(inflater, parent, false)
            )
            TYPE_SECTION_FOOTER -> SectionFooterVH(
                ItemTimelineSectionFooterBinding.inflate(inflater, parent, false)
            )
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is SectionHeaderVH -> holder.bind(item as TimelineDisplayItem.SectionHeader)
            is BookedActivityVH -> holder.bind(
                item as TimelineDisplayItem.BookedActivity,
                onItemClick,
                onDeleteClick
            )
            is ReservedActivityVH -> holder.bind(
                item as TimelineDisplayItem.BookedActivity,
                onItemClick,
                onDeleteClick,
                onReservationClick ?: {}
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
            is GeneratingStateVH -> holder.bind(item as TimelineDisplayItem.GeneratingState)
            is SectionFooterVH -> { /* No binding needed - just separator */ }
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
            oldItem is TimelineDisplayItem.Recommendations && newItem is TimelineDisplayItem.Recommendations ->
                oldItem.plan.id == newItem.plan.id
            oldItem is TimelineDisplayItem.ManualPoi && newItem is TimelineDisplayItem.ManualPoi ->
                oldItem.step.id == newItem.step.id
            oldItem is TimelineDisplayItem.EmptyState && newItem is TimelineDisplayItem.EmptyState ->
                true
            oldItem is TimelineDisplayItem.GeneratingState && newItem is TimelineDisplayItem.GeneratingState ->
                true
            oldItem is TimelineDisplayItem.SectionFooter && newItem is TimelineDisplayItem.SectionFooter ->
                oldItem.city?.id == newItem.city?.id
            else -> false
        }
    }

    override fun areContentsTheSame(
        oldItem: TimelineDisplayItem,
        newItem: TimelineDisplayItem
    ): Boolean {
        return oldItem == newItem
    }
}
