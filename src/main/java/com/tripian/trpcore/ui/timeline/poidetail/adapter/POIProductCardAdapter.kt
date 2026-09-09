package com.tripian.trpcore.ui.timeline.poidetail.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.tripian.one.api.tour.model.TourProduct
import com.tripian.trpcore.R
import com.tripian.trpcore.databinding.ItemPoiProductCardBinding
import com.tripian.trpcore.ui.timeline.adapter.TimelineCellBinder
import com.tripian.trpcore.util.FormatUtils
import com.tripian.trpcore.util.LanguageConst
import java.text.NumberFormat
import java.util.Locale

/**
 * POIProductCardAdapter
 * Horizontal RecyclerView adapter for POI detail activities section
 */
class POIProductCardAdapter(
    private val getLanguage: (String) -> String,
    private val onItemClicked: (TourProduct) -> Unit
) : ListAdapter<TourProduct, POIProductCardAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPoiProductCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = currentList.getOrNull(position) ?: return
        holder.bind(product)
    }

    inner class ViewHolder(
        private val binding: ItemPoiProductCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: TourProduct) {
            val cornerRadius = binding.root.context.resources.getDimensionPixelSize(R.dimen.trp_poi_product_card_corner_radius)
            Glide.with(binding.root.context)
                .load(product.images?.firstOrNull()?.url)
                .placeholder(R.color.trp_grey_10)
                .error(R.color.trp_grey_10)
                .transition(DrawableTransitionOptions.withCrossFade())
                .transform(RoundedCorners(cornerRadius))
                .into(binding.ivProductImage)

            binding.tvProductTitle.text = product.title

            val turkishLocale = Locale("tr", "TR")
            val reviewCountFormat = NumberFormat.getNumberInstance(turkishLocale)
            val rating = product.rating
            val reviewCount = product.ratingCount
            if (rating != null && rating > 0) {
                binding.tvRating.text = String.format(turkishLocale, "%.1f", rating)

                if (reviewCount != null && reviewCount > 0) {
                    val opinionsText = getLanguage(LanguageConst.ADD_PLAN_OPINIONS)
                    binding.tvRatingCount.text = "${reviewCountFormat.format(reviewCount)} $opinionsText"
                    binding.tvRatingCount.visibility = View.VISIBLE
                } else {
                    binding.tvRatingCount.visibility = View.GONE
                }

                binding.llRating.visibility = View.VISIBLE
            } else {
                binding.llRating.visibility = View.GONE
            }

            val isRefundable = product.tags?.contains(TAG_FULL_REFUNDABLE) == true
            binding.tvCancellation.text = if (isRefundable) {
                getLanguage(LanguageConst.ADD_PLAN_FREE_CANCELLATION)
            } else {
                null
            }

            TimelineCellBinder.bindPrice(
                priceRow = binding.llPriceRow,
                fromLabel = binding.tvFromLabel,
                priceView = binding.tvPrice,
                price = product.currentPrice ?: product.price,
                currency = product.currency
            )

            binding.root.setOnClickListener {
                onItemClicked(product)
            }
        }
    }

    companion object {
        private const val TAG_FULL_REFUNDABLE = "full_refundable"

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<TourProduct>() {
            override fun areItemsTheSame(oldItem: TourProduct, newItem: TourProduct): Boolean {
                return oldItem.productId == newItem.productId
            }

            override fun areContentsTheSame(oldItem: TourProduct, newItem: TourProduct): Boolean {
                return oldItem.productId == newItem.productId &&
                    oldItem.title == newItem.title &&
                    oldItem.price == newItem.price &&
                    oldItem.currentPrice == newItem.currentPrice
            }
        }
    }
}
