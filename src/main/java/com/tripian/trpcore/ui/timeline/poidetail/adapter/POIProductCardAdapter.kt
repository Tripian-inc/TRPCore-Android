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
import com.tripian.one.api.pois.model.Product
import com.tripian.trpcore.R
import com.tripian.trpcore.databinding.ItemPoiProductCardBinding
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
    private val onItemClicked: (Product) -> Unit
) : ListAdapter<Product, POIProductCardAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPoiProductCardBinding.inflate(
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
        private val binding: ItemPoiProductCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            // Image
            val cornerRadius = binding.root.context.resources.getDimensionPixelSize(R.dimen.poi_product_card_corner_radius)
            Glide.with(binding.root.context)
                .load(product.image)
                .placeholder(R.color.trp_grey_10)
                .error(R.color.trp_grey_10)
                .transition(DrawableTransitionOptions.withCrossFade())
                .transform(RoundedCorners(cornerRadius))
                .into(binding.ivProductImage)

            // Title
            binding.tvProductTitle.text = product.title ?: ""

            val turkishLocale = Locale("tr", "TR")
            val reviewCountFormat = NumberFormat.getNumberInstance(turkishLocale)
            // Rating
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

            // Cancellation - from poi.additionalData.cancellation or default "Free cancellation"
            val cancellation = if (product.info?.contains("non_refundable") != true) {
                getLanguage(LanguageConst.ADD_PLAN_FREE_CANCELLATION)
            } else null
            binding.tvCancellation.text = cancellation

            // Price - from poi.additionalData.price and poi.additionalData.currency
            val price = product.price
            val currency = product.currency ?: "EUR"
            if (price != null && price > 0) {
                binding.tvFromLabel.text = getLanguage(LanguageConst.FROM) + " "
                binding.tvPrice.text = FormatUtils.formatPriceWithCurrency(price.toDouble(), currency)
                binding.llPriceRow.visibility = View.VISIBLE
            } else {
                binding.llPriceRow.visibility = View.GONE
            }

            // Click listener
            binding.root.setOnClickListener {
                onItemClicked(product)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Product>() {
            override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
                return oldItem.id == newItem.id && oldItem.title == newItem.title
            }
        }
    }
}
