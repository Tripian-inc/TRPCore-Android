package com.tripian.trpcore.ui.timeline.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.tripian.trpcore.R
import com.tripian.trpcore.databinding.ItemMapBottomCardBinding

/**
 * Data model for map bottom list items
 */
data class MapBottomItem(
    val id: String,
    val order: Int,
    val title: String,
    val imageUrl: String?,
    val date: String?,
    val time: String?,
    val type: String,  // "step", "booked", "reserved", "manual"
    val stepType: String? = null  // "poi" or "activity" - only for step type items
)

/**
 * Adapter for horizontal item list at bottom of map view.
 * Shows timeline items as cards with order badge, thumbnail, title, and date/time.
 */
class MapBottomListAdapter(
    private val onItemClicked: (MapBottomItem) -> Unit
) : ListAdapter<MapBottomItem, MapBottomListAdapter.ViewHolder>(MapBottomItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMapBottomCardBinding.inflate(
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
        private val binding: ItemMapBottomCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MapBottomItem) {
            // Order badge
            binding.tvOrderBadge.text = item.order.toString()

            // Title
            binding.tvTitle.text = item.title

            // Thumbnail image
            if (!item.imageUrl.isNullOrEmpty()) {
                val cornerRadius = binding.root.context.resources.getDimensionPixelSize(R.dimen.corner_radius_3dp)
                Glide.with(binding.ivThumbnail)
                    .load(item.imageUrl)
                    .transform(CenterCrop(), RoundedCorners(cornerRadius))
                    .placeholder(R.color.grey_10)
                    .into(binding.ivThumbnail)
            } else {
                binding.ivThumbnail.setImageResource(R.color.grey_10)
            }

            // Date and time
            if (!item.date.isNullOrEmpty() || !item.time.isNullOrEmpty()) {
                binding.llDateTime.visibility = View.VISIBLE
                binding.tvDate.text = item.date ?: ""
                binding.tvTime.text = item.time ?: ""

                // Hide date views if no date
                if (item.date.isNullOrEmpty()) {
                    binding.tvDate.visibility = View.GONE
                } else {
                    binding.tvDate.visibility = View.VISIBLE
                }

                // Hide time views if no time
                if (item.time.isNullOrEmpty()) {
                    binding.tvTime.visibility = View.GONE
                } else {
                    binding.tvTime.visibility = View.VISIBLE
                }
            } else {
                binding.llDateTime.visibility = View.GONE
            }

            // Click listener
            binding.root.setOnClickListener {
                onItemClicked(item)
            }
        }
    }

    /**
     * DiffUtil callback for efficient list updates
     */
    private class MapBottomItemDiffCallback : DiffUtil.ItemCallback<MapBottomItem>() {
        override fun areItemsTheSame(oldItem: MapBottomItem, newItem: MapBottomItem): Boolean {
            return oldItem.id == newItem.id && oldItem.order == newItem.order
        }

        override fun areContentsTheSame(oldItem: MapBottomItem, newItem: MapBottomItem): Boolean {
            return oldItem == newItem
        }
    }
}
