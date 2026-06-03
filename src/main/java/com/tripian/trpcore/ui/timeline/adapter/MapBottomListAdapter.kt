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
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.ItemMapBottomCardBinding
import com.tripian.trpcore.util.LanguageConst

/**
 * Data model for map bottom list items
 */
data class MapBottomItem(
    val id: String,
    val order: Int,
    val title: String,
    val imageUrl: String?,
    val time: String?,
    val type: String,  // "step", "booked", "reserved", "manual", "flexible"
    val stepType: String? = null,  // "poi" or "activity" - only for step type items
    val isSelected: Boolean = false,
    val cityIndex: Int = 0,  // 0 = first city, 1+ = secondary cities (for different badge colors)
    val isFlexible: Boolean = false,  // true → render order chip as "−", never selectable as map focus
    val cityId: Int? = null,  // city id for the item — used to center the camera when the item has no marker
    val cityName: String? = null,  // displayed above the title
    val isNoLocation: Boolean = false  // when true → show the "no exact location" badge
)

/**
 * Adapter for horizontal item list at bottom of map view.
 * Shows timeline items as cards with order badge, thumbnail, title, and date/time.
 */
class MapBottomListAdapter(
    private val onItemClicked: (MapBottomItem) -> Unit
) : ListAdapter<MapBottomItem, MapBottomListAdapter.ViewHolder>(MapBottomItemDiffCallback()) {

    // Only one item can be selected across the whole list — track its id globally.
    private var selectedItemId: String? = null

    /**
     * Submits a new list and tracks the single initially selected item across the
     * whole list (regardless of city).
     */
    override fun submitList(list: List<MapBottomItem>?) {
        selectedItemId = list?.firstOrNull { it.isSelected }?.id
        super.submitList(list)
    }

    /**
     * Selects an item by its ID and updates the list.
     * Deselects any previously selected item across the entire list — only one
     * item can be selected at a time, regardless of city.
     *
     * @param itemId The ID of the item to select
     */
    fun selectItem(itemId: String) {
        if (selectedItemId == itemId) return
        if (currentList.none { it.id == itemId }) return

        val prevSelectedId = selectedItemId
        val updatedList = currentList.map { item ->
            when {
                item.id == itemId -> item.copy(isSelected = true)
                item.id == prevSelectedId -> item.copy(isSelected = false)
                else -> item
            }
        }
        selectedItemId = itemId
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
            // Order badge — flexible items use U+2212 minus, matching FlexibleActivityVH chip
            binding.tvOrderBadge.text = if (item.isFlexible) "−" else item.order.toString()

            // Apply selection styling to badge - same color for all cities
            if (item.isSelected) {
                binding.tvOrderBadge.background = ContextCompat.getDrawable(
                    binding.root.context,
                    R.drawable.trp_bg_marker_red
                )
                binding.tvOrderBadge.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.trp_white)
                )
            } else {
                binding.tvOrderBadge.background = ContextCompat.getDrawable(
                    binding.root.context,
                    R.drawable.trp_bg_marker_white
                )
                binding.tvOrderBadge.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.trp_black_soft)
                )
            }

            // City name above title
            if (!item.cityName.isNullOrBlank()) {
                binding.tvCityName.visibility = View.VISIBLE
                binding.tvCityName.text = item.cityName
            } else {
                binding.tvCityName.visibility = View.GONE
            }

            // Title
            binding.tvTitle.text = item.title

            // Thumbnail image
            if (!item.imageUrl.isNullOrEmpty()) {
                val cornerRadius = binding.root.context.resources.getDimensionPixelSize(R.dimen.trp_corner_radius_3dp)
                Glide.with(binding.ivThumbnail)
                    .load(item.imageUrl)
                    .transform(CenterCrop(), RoundedCorners(cornerRadius))
                    .placeholder(R.color.trp_grey_10)
                    .into(binding.ivThumbnail)
            } else {
                binding.ivThumbnail.setImageResource(R.color.trp_grey_10)
            }

            // No-exact-location badge — shared include used by timeline cells
            binding.noLocationBadge.llNoLocationBadge.visibility = if (item.isNoLocation) {
                binding.noLocationBadge.tvNoLocationLabel.text = TRPCore.core.miscRepository
                    .getLanguageValueForKey(LanguageConst.TIMELINE_NO_EXACT_LOCATION)
                View.VISIBLE
            } else {
                View.GONE
            }

            // Start time row.
            // Flexible items: render the short "Flexible" label in place of the time —
            // the clock icon stays since this row is the time slot for the item.
            when {
                item.isFlexible -> {
                    binding.llStartTime.visibility = View.VISIBLE
                    binding.tvTime.text = TRPCore.core.miscRepository
                        .getLanguageValueForKey(LanguageConst.TIMELINE_FLEXIBLE_SHORT)
                        .ifBlank { "Flexible" }
                }
                !item.time.isNullOrEmpty() -> {
                    binding.llStartTime.visibility = View.VISIBLE
                    binding.tvTime.text = item.time
                }
                else -> {
                    binding.llStartTime.visibility = View.GONE
                }
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
