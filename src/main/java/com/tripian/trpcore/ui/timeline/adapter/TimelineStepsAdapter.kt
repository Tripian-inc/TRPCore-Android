package com.tripian.trpcore.ui.timeline.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tripian.one.api.timeline.model.TimelineStep
import com.tripian.trpcore.databinding.ItemTimelineStepActivityBinding
import com.tripian.trpcore.databinding.ItemTimelineStepPoiBinding
import com.tripian.trpcore.databinding.ItemTimelineStepRouteSeparatorBinding
import com.tripian.trpcore.domain.model.timeline.StepRouteInfo

/**
 * TimelineStepsAdapter
 * Shows steps and route separators within Recommendations
 * Supports three different view types:
 * - Route Separator -> StepRouteSeparatorVH (distance/duration between steps)
 * - POI Step -> StepPoiVH (manual_poi style with change time + delete buttons)
 * - Activity Step -> StepActivityVH (reserved_activity style with rating, duration, price)
 *
 * @param startingOrder The starting order number for steps (for sequential numbering)
 * @param onStepClick Callback for step click events
 * @param onChangeTimeClick Callback for change time button clicks (POI type only)
 * @param onDeleteClick Callback for delete button clicks
 * @param onReservationClick Callback for reservation button clicks (Activity type only)
 */
class TimelineStepsAdapter(
    private val startingOrder: Int = 1,
    private val onStepClick: ((TimelineStep) -> Unit)? = null,
    private val onChangeTimeClick: ((TimelineStep) -> Unit)? = null,
    private val onDeleteClick: ((TimelineStep) -> Unit)? = null,
    private val onReservationClick: ((TimelineStep) -> Unit)? = null
) : ListAdapter<TimelineStepItem, RecyclerView.ViewHolder>(StepItemDiffCallback()) {

    companion object {
        private const val TYPE_ROUTE_SEPARATOR = 0
        private const val TYPE_POI = 1
        private const val TYPE_ACTIVITY = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = getItem(position)) {
            is TimelineStepItem.RouteSeparator -> TYPE_ROUTE_SEPARATOR
            is TimelineStepItem.Step -> {
                if (item.step.stepType == "activity") TYPE_ACTIVITY else TYPE_POI
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_ROUTE_SEPARATOR -> StepRouteSeparatorVH(
                ItemTimelineStepRouteSeparatorBinding.inflate(inflater, parent, false)
            )
            TYPE_ACTIVITY -> StepActivityVH(
                ItemTimelineStepActivityBinding.inflate(inflater, parent, false)
            )
            else -> StepPoiVH(
                ItemTimelineStepPoiBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is TimelineStepItem.RouteSeparator -> {
                (holder as StepRouteSeparatorVH).bind(item.routeInfo)
            }
            is TimelineStepItem.Step -> {
                when (holder) {
                    is StepPoiVH -> holder.bind(
                        step = item.step,
                        order = item.order,
                        onStepClick = onStepClick,
                        onChangeTimeClick = onChangeTimeClick,
                        onDeleteClick = onDeleteClick
                    )
                    is StepActivityVH -> holder.bind(
                        step = item.step,
                        order = item.order,
                        onStepClick = onStepClick,
                        onDeleteClick = onDeleteClick,
                        onReservationClick = onReservationClick
                    )
                }
            }
        }
    }

    /**
     * Submit a list of TimelineStep without route info (backward compatibility)
     * Converts to TimelineStepItem.Step list
     */
    fun submitStepList(steps: List<TimelineStep>) {
        val items = steps.mapIndexed { index, step ->
            TimelineStepItem.Step(step, startingOrder + index)
        }
        submitList(items)
    }

    /**
     * Submit a list of TimelineStepItem with interleaved route separators
     */
    fun submitStepItemList(items: List<TimelineStepItem>) {
        submitList(items)
    }
}

/**
 * DiffUtil callback for TimelineStepItem
 */
class StepItemDiffCallback : DiffUtil.ItemCallback<TimelineStepItem>() {
    override fun areItemsTheSame(oldItem: TimelineStepItem, newItem: TimelineStepItem): Boolean {
        return when {
            oldItem is TimelineStepItem.Step && newItem is TimelineStepItem.Step ->
                oldItem.step.id == newItem.step.id
            oldItem is TimelineStepItem.RouteSeparator && newItem is TimelineStepItem.RouteSeparator ->
                oldItem.routeInfo.fromStepId == newItem.routeInfo.fromStepId &&
                        oldItem.routeInfo.toStepId == newItem.routeInfo.toStepId
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: TimelineStepItem, newItem: TimelineStepItem): Boolean {
        return oldItem == newItem
    }
}

/**
 * Legacy DiffUtil callback for TimelineStep (backward compatibility)
 */
class StepDiffCallback : DiffUtil.ItemCallback<TimelineStep>() {
    override fun areItemsTheSame(oldItem: TimelineStep, newItem: TimelineStep): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: TimelineStep, newItem: TimelineStep): Boolean {
        return oldItem == newItem
    }
}
