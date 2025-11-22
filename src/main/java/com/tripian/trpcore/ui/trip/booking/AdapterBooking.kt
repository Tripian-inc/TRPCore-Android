package com.tripian.trpcore.ui.trip.booking

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.tripian.trpcore.R
import com.tripian.trpcore.util.extensions.dp2Px
import com.tripian.one.api.bookings.model.Reservation
import com.tripian.trpcore.repository.MiscRepository
import com.tripian.trpcore.util.LanguageConst

/**
 * Created by semihozkoroglu on 19.08.2020.
 */
abstract class AdapterBooking(val context: Context, val items: List<Reservation>, val miscRepository: MiscRepository) : RecyclerView.Adapter<AdapterBooking.Place>() {

    private var ROUND_CORNER: Float = dp2Px(12f)

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    abstract fun onClickedPlace(reservation: Reservation)
    abstract fun onClickedCancel(reservation: Reservation)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Place {
        return Place(inflater.inflate(R.layout.item_booking, null))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: Place, position: Int) {
        val item = items[position]

        with(holder) {
            btnCancel.text = miscRepository.getLanguageValueForKey(LanguageConst.CANCEL)
            tvCityName.text = item.value?.data?.shoppingCart?.cityName
            tvTitle.text = item.value?.data?.shoppingCart?.tourName
//            tvDescription.text = item.description

            if (!TextUtils.isEmpty(item.value?.data?.shoppingCart?.tourImage)) {
                Glide.with(context).load(item.value?.data?.shoppingCart?.tourImage)
                    .apply(RequestOptions().transform(CenterCrop(), RoundedCorners(ROUND_CORNER.toInt())))
                    .into(imPoi)
            }

            btnCancel.setOnClickListener { onClickedCancel(item) }
            root.setOnClickListener { onClickedPlace(item) }
        }
    }

    class Place constructor(vi: View) : RecyclerView.ViewHolder(vi) {
        val root: RelativeLayout = vi.findViewById(R.id.root)
        val imPoi: ImageView = vi.findViewById(R.id.imPoi)
        val btnCancel: TextView = vi.findViewById(R.id.btnCancel)
        val tvDescription: TextView = vi.findViewById(R.id.tvDescription)
        val tvTitle: TextView = vi.findViewById(R.id.tvTitle)
        val tvCityName: TextView = vi.findViewById(R.id.tvCityName)
    }
}