package com.tripian.trpcore.ui.timeline.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tripian.one.api.timeline.model.TimelineStep
import com.tripian.trpcore.R
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.ItemTimelineStepPoiBinding
import com.tripian.trpcore.domain.model.timeline.toDate
import com.tripian.trpcore.util.LanguageConst
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * StepPoiVH
 * ViewHolder for POI type steps within Recommendations
 * Shows: order-time, image, title, rating, category, change time + delete buttons
 */
class StepPoiVH(
    private val binding: ItemTimelineStepPoiBinding
) : RecyclerView.ViewHolder(binding.root) {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.US)

    /** Yields comma as decimal separator, dot as thousand separator. */
    private val turkishLocale = Locale("tr", "TR")
    private val reviewCountFormat = NumberFormat.getNumberInstance(turkishLocale)

    /**
     * Helper function for localization
     */
    private fun getLanguage(key: String): String {
        return try {
            TRPCore.core.miscRepository.getLanguageValueForKey(key)
        } catch (e: Exception) {
            key
        }
    }

    /**
     * Binds the step. The rating row is intentionally always hidden, so the
     * category badge uses a tighter 4dp top margin.
     */
    fun bind(
        step: TimelineStep,
        order: Int,
        onStepClick: ((TimelineStep) -> Unit)?,
        onChangeTimeClick: ((TimelineStep) -> Unit)?,
        onDeleteClick: ((TimelineStep) -> Unit)?,
        hasConflict: Boolean = false,
        showTimeOverlapText: Boolean = false
    ) {
        val poi = step.poi

        binding.tvOrder.text = order.toString()

        if (hasConflict) {
            binding.orderTimeContainer.setBackgroundResource(R.drawable.trp_bg_order_time_container_conflict)
            binding.tvOrder.setBackgroundResource(R.drawable.trp_bg_step_order_conflict)
        } else {
            binding.orderTimeContainer.setBackgroundResource(R.drawable.trp_bg_order_time_container)
            binding.tvOrder.setBackgroundResource(R.drawable.trp_bg_step_order_new)
        }

        val startTime = step.startDateTimes?.toDate()
        val endTime = step.endDateTimes?.toDate()
        if (startTime != null && endTime != null) {
            val timeText = "${timeFormat.format(startTime)} - ${timeFormat.format(endTime)}"
            binding.tvTime.text = if (showTimeOverlapText) {
                val overlapText = getLanguage(LanguageConst.TIME_OVERLAP)
                TimeOverlapTextBuilder.build(binding.tvTime.context, timeText, overlapText)
            } else {
                timeText
            }
            binding.tvTime.visibility = View.VISIBLE
        } else if (startTime != null) {
            binding.tvTime.text = timeFormat.format(startTime)
            binding.tvTime.visibility = View.VISIBLE
        } else {
            binding.tvTime.visibility = View.GONE
        }

        binding.tvTitle.text = poi?.name ?: ""

        TimelineCellBinder.loadPoiImage(binding.ivImage, poi?.image?.url)

        binding.llRating.visibility = View.GONE

        poi?.category?.firstOrNull()?.name?.let { category ->
            binding.tvCategory.text = category
            binding.tvCategory.visibility = View.VISIBLE
        } ?: run {
            binding.tvCategory.visibility = View.GONE
        }

        val density = binding.root.context.resources.displayMetrics.density
        (binding.tvCategory.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
            params.topMargin = (4 * density).toInt()
            binding.tvCategory.layoutParams = params
        }

        binding.root.setOnClickListener {
            onStepClick?.invoke(step)
        }

        binding.btnChangeTime.setOnClickListener {
            onChangeTimeClick?.invoke(step)
        }

        binding.btnDelete.setOnClickListener {
            onDeleteClick?.invoke(step)
        }
    }
}
