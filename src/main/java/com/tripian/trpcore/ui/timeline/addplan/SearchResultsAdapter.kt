package com.tripian.trpcore.ui.timeline.addplan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.R
import com.tripian.trpcore.databinding.ItemSavedActivityBinding
import com.tripian.trpcore.domain.model.PlaceAutocomplete

/**
 * Adapter for Google Places search results in starting point selection
 */
class SearchResultsAdapter(
    private val onItemClick: (PlaceAutocomplete) -> Unit
) : ListAdapter<PlaceAutocomplete, SearchResultsAdapter.ViewHolder>(PlaceAutocompleteDiffCallback()) {

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

        fun bind(place: PlaceAutocomplete) {
            binding.ivIcon.setImageResource(R.drawable.ic_location_pin)
            binding.tvName.text = place.area ?: ""
            binding.tvLocation.text = place.address ?: ""
        }
    }

    class PlaceAutocompleteDiffCallback : DiffUtil.ItemCallback<PlaceAutocomplete>() {
        override fun areItemsTheSame(oldItem: PlaceAutocomplete, newItem: PlaceAutocomplete): Boolean {
            return oldItem.placeId == newItem.placeId
        }

        override fun areContentsTheSame(oldItem: PlaceAutocomplete, newItem: PlaceAutocomplete): Boolean {
            return oldItem.placeId == newItem.placeId &&
                    oldItem.area == newItem.area &&
                    oldItem.address == newItem.address
        }
    }
}
