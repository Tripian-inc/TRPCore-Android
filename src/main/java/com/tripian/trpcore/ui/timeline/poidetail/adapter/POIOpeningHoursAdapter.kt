package com.tripian.trpcore.ui.timeline.poidetail.adapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.R
import com.tripian.trpcore.databinding.ItemPoiOpeningHourBinding
import com.tripian.trpcore.ui.timeline.poidetail.OpeningHourItem

/**
 * POIOpeningHoursAdapter
 * Vertical RecyclerView adapter for opening hours list
 */
class POIOpeningHoursAdapter : ListAdapter<OpeningHourItem, POIOpeningHoursAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPoiOpeningHourBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemPoiOpeningHourBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: OpeningHourItem) {
            binding.tvDayName.text = item.dayName
            binding.tvHours.text = item.hours

            // Style closed days differently
            if (item.isClosed) {
                binding.tvHours.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.text_secondary)
                )
                binding.tvHours.setTypeface(null, Typeface.ITALIC)
            } else {
                binding.tvHours.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.text_primary)
                )
                binding.tvHours.setTypeface(null, Typeface.NORMAL)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<OpeningHourItem>() {
            override fun areItemsTheSame(oldItem: OpeningHourItem, newItem: OpeningHourItem): Boolean {
                return oldItem.dayName == newItem.dayName
            }

            override fun areContentsTheSame(oldItem: OpeningHourItem, newItem: OpeningHourItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}
