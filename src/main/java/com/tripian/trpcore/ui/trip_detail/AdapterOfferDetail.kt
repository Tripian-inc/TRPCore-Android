package com.tripian.trpcore.ui.trip_detail

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Color
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.tripian.gyg.util.extensions.offerDate
import com.tripian.one.api.offers.model.Offer
import com.tripian.trpcore.R
import com.tripian.trpcore.repository.MiscRepository
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.dp2Px
import com.tripian.trpcore.util.extensions.formatDate2String
import com.tripian.trpcore.util.extensions.formatDate2Timestamp
import com.tripian.trpcore.util.extensions.getDateFromComponents
import com.tripian.trpcore.util.widget.ImageView
import com.tripian.trpcore.util.widget.InView
import com.tripian.trpcore.util.widget.TextView
import java.util.Calendar

/**
 * Created by semihozkoroglu on 23.04.2021.
 */
abstract class AdapterOfferDetail(
    val context: Context, val items: List<Offer>,
    private val miscRepository: MiscRepository
) :
    RecyclerView.Adapter<AdapterOfferDetail.OfferDetailHolder>() {

    private val inflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    abstract fun onItemOptIn(item: Offer, claimDate: String)
    abstract fun onItemRemoved(item: Offer)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfferDetailHolder {
        return OfferDetailHolder(inflater.inflate(R.layout.item_offer_detail, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: OfferDetailHolder, position: Int) {
        val item = items[position]

        with(holder) {
            tvTitle.text = item.title
            val fromText =
                miscRepository.getLanguageValueForKey(LanguageConst.FROM) + ": " + offerDate(item.timeframe?.start)
            val toText =
                miscRepository.getLanguageValueForKey(LanguageConst.TO) + ": " + offerDate(item.timeframe?.end)
            tvFrom.text = fromText
            tvTo.text = toText

            tvOfferType.text = miscRepository.getLanguageValueForKey(LanguageConst.OFFER_CLAIM)
//            if (item.productType != null && item.productType!!.isDineIn()) {
//                context.getString(R.string.dine_in)
//            } else if (item.productType != null && item.productType!!.isPickUp()) {
//                context.getString(R.string.pick_up)
//            } else {
//                ""
//            }

            if (!TextUtils.isEmpty(item.caption)) {
                tvDescriptionSpend.visibility = View.VISIBLE
                tvDescriptionSpend.text = item.caption
            } else {
                tvDescriptionSpend.visibility = View.GONE
            }

            if (!TextUtils.isEmpty(item.imageUrl)) {
                Glide.with(context).load(item.imageUrl)
                    .apply(
                        RequestOptions().transform(
                            CenterCrop(),
                            RoundedCorners(dp2Px(16f).toInt())
                        )
                    )
                    .into(imImage)
            }

            if (item.optIn) {
                inView.setOnClickListener {
                    onItemRemoved(item)
                }
            } else {
                inView.setOnClickListener {
                    val startDate = formatDate2String(item.timeframe?.start)
                    val endDate = formatDate2String(item.timeframe?.end)

                    if (TextUtils.equals(startDate, endDate) || TextUtils.isEmpty(startDate)) {
                        onItemOptIn(item, startDate)

//                        item.optIn = true
//                        notifyDataSetChanged()
                    } else {
                        showDatePicker(
                            { _: DatePicker?, year: Int, monthOfYear: Int, dayOfMonth: Int ->
                                val claimDate = getDateFromComponents(year, monthOfYear, dayOfMonth)

                                onItemOptIn(item, claimDate)

//                                item.optIn = true
//                                notifyDataSetChanged()
                            },
                            formatDate2Timestamp(item.timeframe?.start),
                            formatDate2Timestamp(item.timeframe?.end)
                        )
                    }
                }
            }

            inView.offerEnable(item.optIn)
        }
    }

    private fun showDatePicker(
        listener: DatePickerDialog.OnDateSetListener,
        minDate: Long,
        maxDate: Long
    ) {
        val currentCalendar = Calendar.getInstance()

        val datePicker = DatePickerDialog(
            context,
            listener,
            currentCalendar[Calendar.YEAR],
            currentCalendar[Calendar.MONTH],
            currentCalendar[Calendar.DAY_OF_MONTH]
        )

        val minCalendar = Calendar.getInstance()
        minCalendar.timeInMillis = minDate
        datePicker.datePicker.minDate = minCalendar.timeInMillis

        val maxCalendar = Calendar.getInstance()
        maxCalendar.timeInMillis = maxDate
        datePicker.datePicker.maxDate = maxCalendar.timeInMillis

        datePicker.show()
        datePicker.getButton(DatePickerDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK)
        datePicker.getButton(DatePickerDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK)
    }

    class OfferDetailHolder(vi: View) : RecyclerView.ViewHolder(vi) {
        val tvTitle = vi.findViewById<TextView>(R.id.tvTitle)
        val inView = vi.findViewById<InView>(R.id.inView)
        val imImage = vi.findViewById<ImageView>(R.id.imOffer)
        val tvFrom = vi.findViewById<TextView>(R.id.tvFrom)
        val tvTo = vi.findViewById<TextView>(R.id.tvTo)
        val tvDescriptionSpend = vi.findViewById<TextView>(R.id.tvDescriptionSpend)
        val tvOfferType = vi.findViewById<TextView>(R.id.tvOfferType)
    }
}