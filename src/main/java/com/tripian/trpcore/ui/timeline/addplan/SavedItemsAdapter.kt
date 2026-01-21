package com.tripian.trpcore.ui.timeline.addplan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.R
import com.tripian.trpcore.databinding.ItemSavedActivityBinding
import com.tripian.trpcore.domain.model.timeline.SavedItem

/**
 * Adapter for saved items (booked activities and favorites) in starting point selection
 */
class SavedItemsAdapter(
    private val onItemClick: (SavedItem) -> Unit
) : ListAdapter<SavedItem, SavedItemsAdapter.ViewHolder>(SavedItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSavedActivityBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemSavedActivityBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(item: SavedItem) {
            binding.ivIcon.setImageResource(R.drawable.ic_location_pin)
            binding.tvName.text = item.title
            binding.tvLocation.text = item.cityName ?: ""
        }
    }

    class SavedItemDiffCallback : DiffUtil.ItemCallback<SavedItem>() {
        override fun areItemsTheSame(oldItem: SavedItem, newItem: SavedItem): Boolean {
            return when {
                oldItem is SavedItem.BookedActivity && newItem is SavedItem.BookedActivity ->
                    oldItem.segment.title == newItem.segment.title
                oldItem is SavedItem.FavouriteActivity && newItem is SavedItem.FavouriteActivity ->
                    oldItem.item.activityId == newItem.item.activityId
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: SavedItem, newItem: SavedItem): Boolean {
            return oldItem == newItem
        }
    }
}
