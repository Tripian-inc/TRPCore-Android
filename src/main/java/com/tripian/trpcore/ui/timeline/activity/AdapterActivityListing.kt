package com.tripian.trpcore.ui.timeline.activity

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.tripian.one.api.tour.model.TourProduct
import com.tripian.trpcore.ui.timeline.common.ActivityCardData
import com.tripian.trpcore.ui.timeline.common.ActivityCardViewHolder

/**
 * AdapterActivityListing
 * RecyclerView adapter for activity/tour listing
 * Uses shared ActivityCardViewHolder to avoid code duplication
 */
class AdapterActivityListing(
    private val getLanguage: (String) -> String,
    private val onAddClicked: (TourProduct) -> Unit
) : ListAdapter<TourProduct, ActivityCardViewHolder>(ActivityDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityCardViewHolder {
        return ActivityCardViewHolder.create(
            parent = parent,
            getLanguage = getLanguage,
            onAddClicked = { cardData ->
                // Find the original TourProduct by id and pass it to callback
                currentList.find { it.id == cardData.id || it.productId == cardData.id }
                    ?.let { onAddClicked(it) }
            }
        )
    }

    override fun onBindViewHolder(holder: ActivityCardViewHolder, position: Int) {
        val tour = getItem(position)
        holder.bind(ActivityCardData.fromTourProduct(tour))
    }

    class ActivityDiffCallback : DiffUtil.ItemCallback<TourProduct>() {
        override fun areItemsTheSame(oldItem: TourProduct, newItem: TourProduct): Boolean {
            return oldItem.productId == newItem.productId
        }

        override fun areContentsTheSame(oldItem: TourProduct, newItem: TourProduct): Boolean {
            return oldItem.productId == newItem.productId &&
                    oldItem.title == newItem.title &&
                    oldItem.price == newItem.price &&
                    oldItem.rating == newItem.rating &&
                    oldItem.tags == newItem.tags
        }
    }
}
