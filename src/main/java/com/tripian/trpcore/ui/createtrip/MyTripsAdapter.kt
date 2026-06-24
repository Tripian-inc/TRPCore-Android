package com.tripian.trpcore.ui.createtrip

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tripian.one.api.timeline.model.Timeline
import com.tripian.trpcore.R
import com.tripian.trpcore.databinding.ItemMyTripCardBinding

/**
 * Adapter for the "My Trips" list ([ACMyTrips]). Each card shows the trip's
 * cover image, city title, country subtitle, date range and (for future trips)
 * a "X days until your trip" pill. Tapping a card opens that timeline.
 */
class MyTripsAdapter(
    private val onTripClicked: (Timeline) -> Unit
) : ListAdapter<Timeline, MyTripsAdapter.TripViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val binding = ItemMyTripCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TripViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) =
        holder.bind(getItem(position))

    inner class TripViewHolder(
        private val binding: ItemMyTripCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(trip: Timeline) {
            binding.tvTitle.text = TripDisplay.cityTitle(trip)

            val country = TripDisplay.countrySubtitle(trip)
            binding.tvSubtitle.text = country
            binding.tvSubtitle.visibility = if (country.isBlank()) View.GONE else View.VISIBLE

            val range = TripDisplay.dateRange(trip)
            binding.tvDates.text = range
            binding.tvDates.visibility = if (range.isBlank()) View.GONE else View.VISIBLE

            val days = TripDisplay.daysUntil(trip)
            if (days != null) {
                binding.tvDaysUntil.text = "$days days until your trip"
                binding.tvDaysUntil.visibility = View.VISIBLE
            } else {
                binding.tvDaysUntil.visibility = View.GONE
            }

            val imageUrl = TripDisplay.imageUrl(trip)
            if (!imageUrl.isNullOrBlank()) {
                Glide.with(binding.ivCover)
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.trp_bg_place_holder_image)
                    .error(R.drawable.trp_bg_place_holder_image)
                    .into(binding.ivCover)
            } else {
                Glide.with(binding.ivCover).clear(binding.ivCover)
                binding.ivCover.setImageResource(R.drawable.trp_bg_place_holder_image)
            }

            binding.cardTrip.setOnClickListener { onTripClicked(trip) }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Timeline>() {
        override fun areItemsTheSame(oldItem: Timeline, newItem: Timeline) =
            oldItem.tripHash == newItem.tripHash

        override fun areContentsTheSame(oldItem: Timeline, newItem: Timeline) =
            oldItem.tripHash == newItem.tripHash &&
                TripDisplay.cityTitle(oldItem) == TripDisplay.cityTitle(newItem) &&
                TripDisplay.dateRange(oldItem) == TripDisplay.dateRange(newItem)
    }
}
