package com.tripian.trpcore.ui.mytrip

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import com.tripian.one.api.trip.model.Trip
import com.tripian.trpcore.R
import com.tripian.trpcore.repository.MiscRepository
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.getMonthAndDay
import com.tripian.trpcore.util.extensions.numberOfDaysBetweenCount
import com.tripian.trpcore.util.extensions.toLargeUrl
import com.tripian.trpcore.util.widget.ImageView
import com.tripian.trpcore.util.widget.TextView

/**
 * Created by semihozkoroglu on 19.08.2020.
 */
abstract class AdapterMyTrip(
    val context: Context,
    val items: List<Trip>,
    val isPastTrip: Boolean = false,
    val miscRepository: MiscRepository
) : RecyclerView.Adapter<AdapterMyTrip.MyTrip>() {

    private val inflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

//    private var ROUND_CORNER: Float = dp2Px(16f)

    abstract fun onClickedItem(trip: Trip)

    abstract fun onClickedEdit(trip: Trip)
    abstract fun onClickedDelete(trip: Trip)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyTrip {
        return MyTrip(inflater.inflate(R.layout.item_my_trip, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MyTrip, position: Int) {
        val item = items[position]

        with(holder) {
            if (isPastTrip) {
                imImage.colorFilter =
                    ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
            }
            if (!TextUtils.isEmpty(item.city?.image?.url?.toLargeUrl())) {
                Glide.with(context).load(item.city?.image?.url?.toLargeUrl())
                    .apply(RequestOptions().transform(CenterCrop()))
                    .into(imImage)
            }
            val cityName = item.city?.name
            val daysCount = item.tripProfile?.arrivalDatetime?.numberOfDaysBetweenCount(
                item.tripProfile?.departureDatetime ?: ""
            ) ?: 1
            val daysText = if (daysCount > 1) {
                "$daysCount ${miscRepository.getLanguageValueForKey(LanguageConst.DAYS)}"
            } else {
                "$daysCount ${miscRepository.getLanguageValueForKey(LanguageConst.DAY)}"
            }
            tvTitle.text = "$daysText $cityName"

            tvCityName.text = "${cityName}, ${item.city?.country?.name}"
            tvDate.text = item.tripProfile?.arrivalDatetime.getMonthAndDay()

            imEdit.isVisible = isPastTrip.not()
            imEdit.setOnClickListener { onClickedEdit(item) }
            imDelete.setOnClickListener { onClickedDelete(item) }

            itemView.setOnClickListener {
                onClickedItem(item)
            }
        }
    }

    class MyTrip(vi: View) : RecyclerView.ViewHolder(vi) {

        val tvTitle: TextView = vi.findViewById(R.id.tvTitle)
        val tvCityName: TextView = vi.findViewById(R.id.tvCityName)
        val tvDate: TextView = vi.findViewById(R.id.tvDate)
        val imImage: ImageView = vi.findViewById(R.id.imImage)
        val imEdit: ImageView = vi.findViewById(R.id.imEdit)
        val imDelete: ImageView = vi.findViewById(R.id.imDelete)
    }
}