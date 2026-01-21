package com.tripian.trpcore.ui.timeline.poi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tripian.one.api.pois.model.Poi
import com.tripian.trpcore.R
import com.tripian.trpcore.databinding.ItemPoiSelectionBinding

/**
 * POISelectionAdapter
 * Adapter for POI selection screen
 */
class POISelectionAdapter(
    private val onPoiClicked: (Poi) -> Unit
) : ListAdapter<Poi, POISelectionAdapter.POIViewHolder>(POIDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): POIViewHolder {
        val binding = ItemPoiSelectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return POIViewHolder(binding)
    }

    override fun onBindViewHolder(holder: POIViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class POIViewHolder(
        private val binding: ItemPoiSelectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(poi: Poi) {
            // Name
            binding.tvName.text = poi.name

            // Category
            poi.category?.firstOrNull()?.name?.let { category ->
                binding.tvCategory.text = category
                binding.tvCategory.visibility = View.VISIBLE
            } ?: run {
                binding.tvCategory.visibility = View.GONE
            }

            // Address
            poi.address?.let { address ->
                binding.tvAddress.text = address
                binding.addressContainer.visibility = View.VISIBLE
            } ?: run {
                binding.addressContainer.visibility = View.GONE
            }

            // Image
            poi.image?.url?.let { url ->
                Glide.with(binding.ivImage)
                    .load(url)
                    .centerCrop()
                    .placeholder(R.drawable.bg_place_holder_image)
                    .into(binding.ivImage)
            } ?: run {
                binding.ivImage.setImageResource(R.drawable.bg_place_holder_image)
            }

            // Rating
            poi.rating?.let { rating ->
                if (rating > 0) {
                    binding.tvRating.text = String.format("%.1f", rating)
                    binding.ratingContainer.visibility = View.VISIBLE
                } else {
                    binding.ratingContainer.visibility = View.GONE
                }
            } ?: run {
                binding.ratingContainer.visibility = View.GONE
            }

            // Click listener
            binding.root.setOnClickListener {
                onPoiClicked(poi)
            }
        }
    }
}

class POIDiffCallback : DiffUtil.ItemCallback<Poi>() {
    override fun areItemsTheSame(oldItem: Poi, newItem: Poi): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Poi, newItem: Poi): Boolean {
        return oldItem == newItem
    }
}
