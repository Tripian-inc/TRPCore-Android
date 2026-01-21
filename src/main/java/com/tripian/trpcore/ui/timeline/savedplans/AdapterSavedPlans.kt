package com.tripian.trpcore.ui.timeline.savedplans

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.databinding.ItemSavedPlansSectionHeaderBinding
import com.tripian.trpcore.domain.model.itinerary.SegmentFavoriteItem
import com.tripian.trpcore.ui.timeline.common.ActivityCardData
import com.tripian.trpcore.ui.timeline.common.ActivityCardViewHolder

/**
 * AdapterSavedPlans
 * RecyclerView adapter for Saved Plans list with city grouping
 * Uses shared ActivityCardViewHolder to avoid code duplication
 */
class AdapterSavedPlans(
    private val getLanguage: (String) -> String,
    private val onAddClicked: (SegmentFavoriteItem) -> Unit
) : ListAdapter<SavedPlansListItem, RecyclerView.ViewHolder>(SavedPlansDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_SECTION_HEADER = 0
        private const val VIEW_TYPE_ACTIVITY = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is SavedPlansListItem.SectionHeader -> VIEW_TYPE_SECTION_HEADER
            is SavedPlansListItem.ActivityItem -> VIEW_TYPE_ACTIVITY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SECTION_HEADER -> {
                val binding = ItemSavedPlansSectionHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                SectionHeaderViewHolder(binding)
            }
            else -> {
                ActivityCardViewHolder.create(
                    parent = parent,
                    getLanguage = getLanguage,
                    onAddClicked = { cardData ->
                        // Find the original SegmentFavoriteItem by id and pass it to callback
                        currentList
                            .filterIsInstance<SavedPlansListItem.ActivityItem>()
                            .find { it.favorite.activityId == cardData.id }
                            ?.favorite
                            ?.let { onAddClicked(it) }
                    }
                )
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is SavedPlansListItem.SectionHeader -> {
                (holder as SectionHeaderViewHolder).bind(item)
            }
            is SavedPlansListItem.ActivityItem -> {
                (holder as ActivityCardViewHolder).bind(ActivityCardData.fromFavorite(item.favorite))
            }
        }
    }

    /**
     * ViewHolder for Section Header (City Name)
     */
    inner class SectionHeaderViewHolder(
        private val binding: ItemSavedPlansSectionHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SavedPlansListItem.SectionHeader) {
            binding.tvCityName.text = item.cityName
        }
    }

    /**
     * DiffUtil callback for efficient list updates
     */
    class SavedPlansDiffCallback : DiffUtil.ItemCallback<SavedPlansListItem>() {
        override fun areItemsTheSame(
            oldItem: SavedPlansListItem,
            newItem: SavedPlansListItem
        ): Boolean {
            return when {
                oldItem is SavedPlansListItem.SectionHeader && newItem is SavedPlansListItem.SectionHeader -> {
                    oldItem.cityId == newItem.cityId
                }
                oldItem is SavedPlansListItem.ActivityItem && newItem is SavedPlansListItem.ActivityItem -> {
                    oldItem.favorite.activityId == newItem.favorite.activityId
                }
                else -> false
            }
        }

        override fun areContentsTheSame(
            oldItem: SavedPlansListItem,
            newItem: SavedPlansListItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}
