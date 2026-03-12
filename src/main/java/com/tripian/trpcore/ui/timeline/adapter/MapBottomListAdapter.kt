package com.tripian.trpcore.ui.timeline.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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
    val stepType: String? = null,  // "poi" or "activity" - only for step type items
    val isSelected: Boolean = false,
    val cityIndex: Int = 0  // 0 = first city, 1+ = secondary cities (for different badge colors)
)

/**
 * Adapter for horizontal item list at bottom of map view.
 * Shows timeline items as cards with order badge, thumbnail, title, and date/time.
 */
class MapBottomListAdapter(
    private val onItemClicked: (MapBottomItem) -> Unit
) : ListAdapter<MapBottomItem, MapBottomListAdapter.ViewHolder>(MapBottomItemDiffCallback()) {

    // Track selected item per city: cityIndex -> itemId
    private var selectedItemIds = mutableMapOf<Int, String>()

    /**
     * Submits a new list and tracks the initially selected items per city.
     */
    override fun submitList(list: List<MapBottomItem>?) {
        selectedItemIds.clear()
        // Track initially selected items per city
        list?.filter { it.isSelected }?.forEach {
            selectedItemIds[it.cityIndex] = it.id
        }
        super.submitList(list)
    }

    /**
     * Selects an item by its ID and updates the list.
     * Only deselects the previously selected item in the same city.
     * Each city can have its own selected item.
     *
     * @param itemId The ID of the item to select
     */
    fun selectItem(itemId: String) {
        val targetItem = currentList.find { it.id == itemId } ?: return
        val cityIndex = targetItem.cityIndex

        if (selectedItemIds[cityIndex] == itemId) return

        val prevSelectedId = selectedItemIds[cityIndex]
        val updatedList = currentList.map { item ->
            when {
                item.id == itemId -> item.copy(isSelected = true)
                item.id == prevSelectedId -> item.copy(isSelected = false)
                else -> item
            }
        }
        selectedItemIds[cityIndex] = itemId
        super.submitList(updatedList)
    }

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

            // Apply selection styling to badge based on city index
            if (item.cityIndex == 0) {
                // First city: black/white style
                if (item.isSelected) {
                    binding.tvOrderBadge.background = ContextCompat.getDrawable(
                        binding.root.context,
                        R.drawable.bg_marker_red
                    )
                    binding.tvOrderBadge.setTextColor(
                        ContextCompat.getColor(binding.root.context, R.color.trp_white)
                    )
                } else {
                    binding.tvOrderBadge.background = ContextCompat.getDrawable(
                        binding.root.context,
                        R.drawable.bg_marker_white
                    )
                    binding.tvOrderBadge.setTextColor(
                        ContextCompat.getColor(binding.root.context, R.color.trp_black_soft)
                    )
                }
            } else {
                // Secondary cities: primary color style
                if (item.isSelected) {
                    binding.tvOrderBadge.background = ContextCompat.getDrawable(
                        binding.root.context,
                        R.drawable.bg_marker_primary
                    )
                    binding.tvOrderBadge.setTextColor(
                        ContextCompat.getColor(binding.root.context, R.color.trp_white)
                    )
                } else {
                    binding.tvOrderBadge.background = ContextCompat.getDrawable(
                        binding.root.context,
                        R.drawable.bg_marker_white_primary
                    )
                    binding.tvOrderBadge.setTextColor(
                        ContextCompat.getColor(binding.root.context, R.color.trp_primary)
                    )
                }
            }

            // Title
            binding.tvTitle.text = item.title

            // Thumbnail image
            if (!item.imageUrl.isNullOrEmpty()) {
                val cornerRadius = binding.root.context.resources.getDimensionPixelSize(R.dimen.corner_radius_3dp)
                Glide.with(binding.ivThumbnail)
                    .load(item.imageUrl)
                    .transform(CenterCrop(), RoundedCorners(cornerRadius))
                    .placeholder(R.color.trp_grey_10)
                    .into(binding.ivThumbnail)
            } else {
                binding.ivThumbnail.setImageResource(R.color.trp_grey_10)
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
