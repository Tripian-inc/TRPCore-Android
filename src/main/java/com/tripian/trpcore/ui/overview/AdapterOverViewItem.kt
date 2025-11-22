package com.tripian.trpcore.ui.overview

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.tripian.trpcore.R
import com.tripian.trpcore.domain.model.ButterflyItem
import com.tripian.trpcore.util.extensions.toSmallUrl
import com.tripian.trpcore.util.widget.ImageView
import com.tripian.trpcore.util.widget.TextView

/**
 * Created by semihozkoroglu on 19.08.2020.
 */
abstract class AdapterOverViewItem(val context: Context, val items: List<ButterflyItem>) : RecyclerView.Adapter<AdapterOverViewItem.ButterflyInner>() {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    abstract fun onClickedItem(item: ButterflyItem)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ButterflyInner {
        return ButterflyInner(inflater.inflate(R.layout.item_overview_item, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ButterflyInner, position: Int) {
        val item = items[position]

        with(holder) {
            tvTitle.text = item.step?.poi?.name

            if (item.step != null && !item.step!!.score.isNullOrEmpty()) {
                tvMatch.visibility = View.VISIBLE
//                tvMatch.text = "${item.step!!.score}% " + context.getString(R.string.match)

                tvMatch.visibility = View.VISIBLE
                tvSplitter.visibility = View.VISIBLE
            } else {
                tvMatch.visibility = View.GONE
                tvSplitter.visibility = View.GONE
            }

            if (!TextUtils.isEmpty(item.category?.name)) {
                tvCategory.text = "${item.category?.type}"

                tvCategory.visibility = View.VISIBLE
                tvSplitter.visibility = View.VISIBLE
            } else {
                tvCategory.visibility = View.GONE
                tvSplitter.visibility = View.GONE
            }

            if (!TextUtils.isEmpty(item.step?.poi?.image?.url)) {
                Glide.with(context).load(item.step?.poi?.image?.url?.toSmallUrl())
                    .apply(RequestOptions().circleCrop())
                    .placeholder(ContextCompat.getDrawable(context, R.drawable.bg_place_holder_image))
                    .into(imPoi)
            }

            root.setOnClickListener { onClickedItem(item) }
        }
    }

    class ButterflyInner constructor(vi: View) : RecyclerView.ViewHolder(vi) {
        val root: RelativeLayout = vi.findViewById(R.id.root)
        val imPoi: ImageView = vi.findViewById(R.id.imPoi)
        val tvTitle: TextView = vi.findViewById(R.id.tvTitle)
        val tvMatch: TextView = vi.findViewById(R.id.tvMatch)
        val tvCategory: TextView = vi.findViewById(R.id.tvCategory)
        val tvSplitter: TextView = vi.findViewById(R.id.tvSplitter)
    }
}