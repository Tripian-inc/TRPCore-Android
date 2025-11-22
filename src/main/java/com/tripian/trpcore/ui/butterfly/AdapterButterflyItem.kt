package com.tripian.trpcore.ui.butterfly

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.tripian.trpcore.R
import com.tripian.trpcore.domain.model.ButterflyItem
import com.tripian.trpcore.util.extensions.dp2Px
import com.tripian.trpcore.util.extensions.generateImgLink
import com.tripian.trpcore.util.extensions.toLargeUrl
import com.tripian.trpcore.util.widget.CardView
import com.tripian.trpcore.util.widget.ImageView
import com.tripian.trpcore.util.widget.TextView

/**
 * Created by semihozkoroglu on 19.08.2020.
 */
abstract class AdapterButterflyItem constructor(val context: Context, val items: ArrayList<ButterflyItem>) : RecyclerView.Adapter<AdapterButterflyItem.ButterflyInner>() {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    private var ROUND_CORNER: Float = dp2Px(6f)

    abstract fun onClickedItem(item: ButterflyItem)

    abstract fun onClickedLike(item: ButterflyItem)

    abstract fun onClickedDislike(item: ButterflyItem)

    abstract fun onClickedUndo(item: ButterflyItem)

    abstract fun onClickedTellUs(item: ButterflyItem)

    abstract fun onClickedClose(item: ButterflyItem)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ButterflyInner {
        return ButterflyInner(inflater.inflate(R.layout.item_butterfly_inner, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ButterflyInner, position: Int) {
        val item = items[position]

        with(holder) {
            tvTitle.text = item.step?.poi?.name

            if (item.step?.poi?.rating != null) {
                rateBar.rating = item.step!!.poi!!.rating
            } else {
                rateBar.visibility = View.GONE
            }

            if (item.step != null && !item.step!!.score.isNullOrEmpty()) {
                tvScore.visibility = View.VISIBLE
//                tvScore.text = "${item.step!!.score}% " + context.getString(R.string.match)
            } else {
                tvScore.visibility = View.GONE
            }

            if (!TextUtils.isEmpty(item.category?.name) && !TextUtils.isEmpty(item.day)) {
                tvTag.text = "${item.category?.type} / ${item.day}"
            } else if (!TextUtils.isEmpty(item.category?.name)) {
                tvTag.text = "${item.category?.type}"
            } else if (!TextUtils.isEmpty(item.day)) {
                tvTag.text = "${item.day}"
            }

            if (!TextUtils.isEmpty(item.step?.poi?.description)) {
                tvDescription.text = item.step?.poi?.description
                tvDescription.visibility = View.VISIBLE
            } else {
                tvDescription.visibility = View.GONE
            }

            val imgLink: String? = item.step?.poi?.image?.url?.generateImgLink(256, 256)
            if (!TextUtils.isEmpty(imgLink)) {
                Glide.with(context).load(imgLink?.toLargeUrl())
                    .apply(RequestOptions().transform(CenterCrop(), GranularRoundedCorners(ROUND_CORNER, ROUND_CORNER, 0f, 0f)))
                    .into(imImage)
            }

            if (!item.isLikeSelected && !item.isDislikeSelected) {
                imLike.visibility = View.VISIBLE
                imDislike.visibility = View.VISIBLE

                imLike.setImageResource(R.drawable.ic_like)
                imDislike.setImageResource(R.drawable.ic_dislike)

                imLike.setOnClickListener {
                    item.isLikeSelected = true

                    onClickedLike(item)

                    notifyDataSetChanged()
                }
                imDislike.setOnClickListener {
                    item.isDislikeSelected = true

                    onClickedDislike(item)

                    notifyDataSetChanged()
                }

                cvTop.visibility = View.GONE
                imTopClose.setOnClickListener(null)

                cvBottom.setOnClickListener { onClickedItem(item) }
            } else {
                imLike.setOnClickListener(null)
                imDislike.setOnClickListener(null)

                if (item.isLikeSelected) {
                    imLike.visibility = View.VISIBLE
                    imLike.setImageResource(R.drawable.ic_like_selected)
                    imDislike.visibility = View.GONE

                    cvTop.visibility = View.GONE
                    imTopClose.setOnClickListener(null)

                    cvBottom.setOnClickListener { onClickedItem(item) }

                    imLike.setOnClickListener {
                        item.isLikeSelected = false
                        item.isDislikeSelected = false
                        item.isClosed = false

                        onClickedUndo(item)

                        notifyDataSetChanged()
                    }
                } else if (item.isDislikeSelected) {
                    imLike.visibility = View.GONE
                    imDislike.visibility = View.VISIBLE
                    imDislike.setImageResource(R.drawable.ic_dislike_selected)

                    if (item.isClosed) {
                        cvTop.visibility = View.GONE

                        cvBottom.setOnClickListener { onClickedItem(item) }
                    } else {
                        cvTop.visibility = View.VISIBLE

                        cvBottom.setOnClickListener(null)
                    }

                    imTopClose.setOnClickListener {
                        item.isClosed = true

                        onClickedClose(item)

                        notifyDataSetChanged()
                    }

                    tvUndo.setOnClickListener {
                        item.isLikeSelected = false
                        item.isDislikeSelected = false
                        item.isClosed = false

                        onClickedUndo(item)

                        notifyDataSetChanged()
                    }

                    tvTellUs.setOnClickListener { onClickedTellUs(item) }
                }
            }
        }
    }

    class ButterflyInner constructor(vi: View) : RecyclerView.ViewHolder(vi) {
        val imImage: ImageView = vi.findViewById(R.id.imImage)
        val tvTitle: TextView = vi.findViewById(R.id.tvTitle)
        val tvTag: TextView = vi.findViewById(R.id.tvTag)
        val rateBar: RatingBar = vi.findViewById(R.id.rateBar)
        val tvDescription: TextView = vi.findViewById(R.id.tvDescription)
        val imLike: ImageView = vi.findViewById(R.id.imLike)
        val imDislike: ImageView = vi.findViewById(R.id.imDislike)
        val imTopClose: ImageView = vi.findViewById(R.id.imTopClose)
        val cvBottom: CardView = vi.findViewById(R.id.cvBottom)
        val cvTop: CardView = vi.findViewById(R.id.cvTop)
        val tvUndo: TextView = vi.findViewById(R.id.tvUndo)
        val tvTellUs: TextView = vi.findViewById(R.id.tvTellUs)
        val tvScore: TextView = vi.findViewById(R.id.tvScore)
    }
}