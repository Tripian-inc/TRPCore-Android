package com.tripian.trpcore.ui.timeline.poilisting

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.tripian.one.api.pois.model.Poi
import com.tripian.trpcore.R
import com.tripian.trpcore.databinding.ItemPoiListingBinding
import com.tripian.trpcore.util.LanguageConst
import java.util.Locale

/**
 * AdapterPOIListing
 * RecyclerView adapter for POI listing
 * iOS Reference: POIListingCell
 */
class AdapterPOIListing(
    private val getLanguage: (String) -> String,
    private val onAddClicked: (Poi) -> Unit,
    private val onItemClicked: ((Poi) -> Unit)? = null
) : ListAdapter<Poi, AdapterPOIListing.POIViewHolder>(POIDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): POIViewHolder {
        val binding = ItemPoiListingBinding.inflate(
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
        private val binding: ItemPoiListingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(poi: Poi) {
            // Name
            binding.tvPOIName.text = poi.name ?: ""

            // Image
            val imageUrl = poi.image?.url
            if (!imageUrl.isNullOrEmpty()) {
                val cornerRadius = binding.root.context.resources.getDimensionPixelSize(R.dimen.corner_radius_3dp)
                Glide.with(binding.ivPOIImage)
                    .load(imageUrl)
                    .transform(CenterCrop(), RoundedCorners(cornerRadius))
                    .placeholder(R.color.grey_10)
                    .into(binding.ivPOIImage)
            } else {
                binding.ivPOIImage.setImageResource(R.color.grey_10)
            }

            // Rating
            val rating = poi.rating
            val ratingCount = poi.ratingCount
            if (rating > 0) {
                binding.llRatingRow.visibility = View.VISIBLE
                // Format rating with comma (e.g., "5,0")
                binding.tvRating.text = String.format(Locale.GERMAN, "%.1f", rating)

                // Rating count with dot as thousand separator (e.g., "620.214 opiniones")
                if (ratingCount != null && ratingCount > 0) {
                    binding.tvRatingCount.visibility = View.VISIBLE
                    val formattedCount = String.format(Locale.GERMAN, "%,d", ratingCount).replace(',', '.')
                    val opinionsText = getLanguage(LanguageConst.ADD_PLAN_OPINIONS)
                    binding.tvRatingCount.text = "$formattedCount $opinionsText"
                } else {
                    binding.tvRatingCount.visibility = View.GONE
                }
            } else {
                binding.llRatingRow.visibility = View.GONE
            }

            // Add button click
            binding.btnAdd.setOnClickListener {
                onAddClicked(poi)
            }

            // Item click - opens POI Detail
            binding.root.setOnClickListener {
                onItemClicked?.invoke(poi)
            }
        }
    }

    class POIDiffCallback : DiffUtil.ItemCallback<Poi>() {
        override fun areItemsTheSame(oldItem: Poi, newItem: Poi): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Poi, newItem: Poi): Boolean {
            return oldItem.id == newItem.id &&
                    oldItem.name == newItem.name &&
                    oldItem.rating == newItem.rating
        }
    }
}
