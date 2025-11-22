package com.tripian.trpcore.ui.trip

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.tripian.trpcore.R
import com.tripian.trpcore.domain.model.MapStep
import com.tripian.trpcore.domain.model.UberModel
import com.tripian.trpcore.repository.MiscRepository
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.StartDragListener
import com.tripian.trpcore.util.extensions.getHoursText
import com.tripian.trpcore.util.extensions.getPriceSpannableString
import com.tripian.trpcore.util.extensions.roundTo
import com.tripian.trpcore.util.extensions.toSmallUrl
import com.tripian.trpcore.util.widget.ImageView
import com.tripian.trpcore.util.widget.TextView
import com.tripian.trpfoundationkit.enums.ReactionType
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Created by semihozkoroglu on 19.08.2020.
 */
abstract class AdapterStep(
    val dragListener: StartDragListener,
    val context: Context,
    var items: List<MapStep>,
    val miscRepository: MiscRepository
) : RecyclerView.Adapter<AdapterStep.SelectDay>() {

    private val inflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    abstract fun onClickedItem(step: MapStep)

    abstract fun onClickedAlternatives(step: MapStep)

    abstract fun onClickedDelete(pos: Int, step: MapStep)

    abstract fun onClickedThumbsUp(pos: Int, step: MapStep)

    abstract fun onClickedThumbsDown(pos: Int, step: MapStep)

    abstract fun onClickedThumbsUndo(pos: Int, step: MapStep)

    abstract fun onClickedChangeTime(step: MapStep)

    abstract fun onClickedUber(uberModel: UberModel)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectDay {
        return SelectDay(inflater.inflate(R.layout.item_step, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemViewType(position: Int): Int {
        if (items[position].homeBase) {
            return 0
        }

        return 1
    }

    private fun createUberInfo(index: Int): UberModel? {
        if (index + 1 < items.size) {
            val pickPoi = items[index].poi
            val dropPoi = items[index + 1].poi

            val pickCoordinate = items[index].coordinate
            val dropCoordinate = items[index].coordinate

            if (pickCoordinate != null && dropCoordinate != null) {
                return UberModel(
                    pickCoordinate,
                    pickPoi?.name ?: "",
                    pickPoi?.address ?: "",
                    dropCoordinate,
                    dropPoi?.name ?: "",
                    dropPoi?.address ?: ""
                )
            }
        }

        return null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: SelectDay, position: Int) {
        val item = items[position]

        with(holder) {
            tvReplace.text = miscRepository.getLanguageValueForKey(LanguageConst.REPLACE)
            tvRemove.text = miscRepository.getLanguageValueForKey(LanguageConst.REMOVE)
            btnBookARide.text = miscRepository.getLanguageValueForKey(LanguageConst.BOOK_A_RIDE)

            tvTitle.text = item.name
            tvOrder.text = "$position"

            viLine.isVisible = (position == itemCount - 1).not()
            btnBookARide.isVisible = false

            if (!TextUtils.isEmpty(item.image) && !item.homeBase) {
                Glide.with(context).load(item.image?.toSmallUrl())
                    .apply(RequestOptions().circleCrop())
                    .placeholder(
                        ContextCompat.getDrawable(
                            context,
                            R.drawable.bg_place_holder_image
                        )
                    )
                    .into(imPoi)
            }

            if (item.homeBase) {
                Glide.with(context).load(R.drawable.ic_map_icon_homebase)
                    .centerInside()
                    .into(imPoi)
                imPoi.setBackgroundResource(R.drawable.bg_circle_black_full)
                imPoi.scaleType = android.widget.ImageView.ScaleType.CENTER
            }

            llChangeTime.isVisible = item.homeBase.not()
            llChangeTime.setOnClickListener { onClickedChangeTime(item) }

            tvCategory.text = item.category
            tvCategory.isVisible = item.category?.isNotEmpty() == true

            tvCategoryPrice.isVisible = item.price != -1

            if (item.price != -1) {
                val str = getPriceSpannableString(
                    color = ContextCompat.getColor(
                        context,
                        R.color.primary
                    ),
                    price = item.price
                )

                tvCategoryPrice.text = str
            }

            llRating.isVisible = item.homeBase.not() && item.isRatingAvailable()
            tvRate.text = item.rating.roundTo(1).toString()
            tvReview.text = "(${item.ratingCount})"
            tvHours.text = item.getHoursText()

            llRouteInfo.isVisible = item.leg != null
            if (item.leg != null) {
                var distance = BigDecimal(item.leg!!.distance!!).divide(BigDecimal(1000.0))

                if (distance == BigDecimal.ZERO) {
                    distance = BigDecimal(0.1)
                }

                val distanceTextKey = LanguageConst.DURATION_CAR

                imDrive.visibility = View.VISIBLE
                if (distance.compareTo(BigDecimal(1.8)) == 1) {
                    imDrive.setImageResource(R.drawable.ic_itinerary_car)
//                    distanceTextKey = LanguageConst.DURATION_CAR
                    createUberInfo(position).let { uber ->
                        btnBookARide.isVisible = uber != null
                        btnBookARide.setOnClickListener { uber?.let { onClickedUber(it) } }
                    }
                } else {
                    btnBookARide.isVisible = false
                    imDrive.setImageResource(R.drawable.ic_itinerary_walk)
                }

                var min = item.leg!!.duration!! / 60
                if (distance.compareTo(BigDecimal(2.0)) == -1) {
                    min = distance.toDouble() * 12
                    if (min < 1.0) {
                        min = 1.0
                    }
                }
                val distanceText = miscRepository.getLanguageValueForKeyWithText(
                    distanceTextKey,
                    listOf("${min.toInt()}", "${distance.setScale(2, RoundingMode.CEILING)}")
                )
                tvDistance.text = distanceText
            }

            if (!item.homeBase) {
                if (item.reaction != null) {
                    llThumbsUpDown.visibility = View.GONE

                    if (TextUtils.equals(
                            item.reaction?.reaction,
                            ReactionType.THUMBS_UP.toString()
                        )
                    ) {
                        llReplace.visibility = View.GONE
                        llRemove.visibility = View.GONE
                        imLikeSelected.visibility = View.VISIBLE
                        imDislikeSelected.visibility = View.GONE

                        imLikeSelected.setOnClickListener { onClickedThumbsUndo(position, item) }
                    } else if (TextUtils.equals(
                            item.reaction?.reaction,
                            ReactionType.THUMBS_DOWN.toString()
                        )
                    ) {
                        llRemove.visibility = View.VISIBLE
                        imLikeSelected.visibility = View.GONE
                        imDislikeSelected.visibility = View.VISIBLE

                        imDislikeSelected.setOnClickListener {
                            onClickedThumbsUndo(position, item)
                        }

                        if (item.alternatives.isNullOrEmpty()) {
                            llReplace.visibility = View.GONE
                            llReplace.setOnClickListener(null)
                        } else {
                            llReplace.visibility = View.VISIBLE
                            llReplace.setOnClickListener { onClickedAlternatives(item) }
                        }

                        llRemove.setOnClickListener { onClickedDelete(position, item) }
                    } else {
                        llReplace.visibility = View.GONE
                        llRemove.visibility = View.GONE
                        imDislikeSelected.visibility = View.GONE
                        imLikeSelected.visibility = View.GONE
                    }
                } else {
                    llThumbsUpDown.visibility = View.VISIBLE
                    llReplace.visibility = View.GONE
                    llRemove.visibility = View.GONE
                    imDislikeSelected.visibility = View.GONE
                    imLikeSelected.visibility = View.GONE

                    imLike.setOnClickListener {
                        onClickedThumbsUp(position, item)
                    }

                    imDislike.setOnClickListener {
                        onClickedThumbsDown(position, item)
                    }
                }
            }


            imAction.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    dragListener.requestDrag(holder)
                }

                false
            }

            if (item.homeBase) {
                tvOrder.visibility = View.GONE
                imAction.visibility = View.GONE
                llCategory.visibility = View.GONE
                llRouteInfo.isVisible = position < 1
            } else {
                tvOrder.visibility = View.VISIBLE
                imAction.visibility = View.VISIBLE
                llCategory.visibility = View.VISIBLE
                llRouteInfo.visibility = View.VISIBLE
            }

            root.setOnClickListener { onClickedItem(item) }
        }
    }

    class SelectDay(vi: View) : RecyclerView.ViewHolder(vi) {
        val root: RelativeLayout = vi.findViewById(R.id.root)
        val viLine: ImageView = vi.findViewById(R.id.viLine)
        val tvTitle: TextView = vi.findViewById(R.id.tvTitle)
        val tvOrder: TextView = vi.findViewById(R.id.tvOrder)
        val imPoi: ImageView = vi.findViewById(R.id.imPoi)
        val imAction: ImageView = vi.findViewById(R.id.imAction)
        val tvCategory: TextView = vi.findViewById(R.id.tvCategory)
        val tvCategoryPrice: TextView = vi.findViewById(R.id.tvCategoryPrice)
        val llRating: LinearLayout = vi.findViewById(R.id.llRating)
        val tvRate: TextView = vi.findViewById(R.id.tvRate)
        val tvReview: TextView = vi.findViewById(R.id.tvReview)
        val llRouteInfo: LinearLayout = vi.findViewById(R.id.llRouteInfo)
        val tvDistance: TextView = vi.findViewById(R.id.tvDistance)
        val imDrive: ImageView = vi.findViewById(R.id.imDrive)
        val btnBookARide: Button = vi.findViewById(R.id.btnBookARide)
        val llCategory: LinearLayout = vi.findViewById(R.id.llCategory)
        val llReplace: LinearLayout = vi.findViewById(R.id.llReplace)
        val llRemove: LinearLayout = vi.findViewById(R.id.llRemove)
        val imLikeSelected: ImageView = vi.findViewById(R.id.imLikeSelected)
        val imDislikeSelected: ImageView = vi.findViewById(R.id.imDislikeSelected)
        val llThumbsUpDown: LinearLayout = vi.findViewById(R.id.llThumbsUpDown)
        val imLike: ImageView = vi.findViewById(R.id.imLike)
        val imDislike: ImageView = vi.findViewById(R.id.imDislike)
        val tvReplace: TextView = vi.findViewById(R.id.tvReplace)
        val tvRemove: TextView = vi.findViewById(R.id.tvRemove)
        val tvHours: TextView = vi.findViewById(R.id.tvHours)
        val llChangeTime: LinearLayout = vi.findViewById(R.id.llChangeTime)
    }
}