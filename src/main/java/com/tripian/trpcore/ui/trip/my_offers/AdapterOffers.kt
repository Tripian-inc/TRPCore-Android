package com.tripian.trpcore.ui.trip.my_offers

import android.annotation.SuppressLint
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
import com.tripian.gyg.util.extensions.optInDate
import com.tripian.gyg.util.extensions.offerDate
import com.tripian.one.api.offers.model.Offer
import com.tripian.one.api.offers.model.isDineIn
import com.tripian.one.api.offers.model.isPickUp
import com.tripian.trpcore.R
import com.tripian.trpcore.repository.MiscRepository
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.dp2Px
import com.tripian.trpcore.util.extensions.formatDate2String
import com.tripian.trpcore.util.extensions.formatDate2Timestamp
import com.tripian.trpcore.util.widget.ImageView
import com.tripian.trpcore.util.widget.InView
import com.tripian.trpcore.util.widget.TextView
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by semihozkoroglu on 23.04.2021.
 */
abstract class AdapterOffers constructor(
    val context: Context, val items: ArrayList<Offer>, val optOutEnable: Boolean = false,
    private val miscRepository: MiscRepository
) :
    RecyclerView.Adapter<AdapterOffers.OfferHolder>() {

    private val inflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    private var ROUND_CORNER: Float = dp2Px(24f)

    abstract fun addOffer(pos: Int, item: Offer, claimDate: String)
    abstract fun removeOffer(pos: Int, item: Offer)

    abstract fun onItemClicked(item: Offer)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfferHolder {
        return OfferHolder(inflater.inflate(R.layout.item_offer_detail, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun notify(pos: Int) {
//        items.removeAt(pos)
        notifyItemRemoved(pos)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: OfferHolder, position: Int) {
        val item = items[position]

        with(holder) {
            tvTitle.text = item.title
//            tvDescription.text = item.title
            tvFrom.text = miscRepository.getLanguageValueForKey(LanguageConst.DATE) + ": " + optInDate(item.optInDate)
            tvTo.text = ""
//            tvFrom.text = context.getString(R.string.offer_from, offerDate(item.timeframe?.start))
//            tvTo.text = context.getString(R.string.offer_to, offerDate(item.timeframe?.end))

//            item.discount()?.let {
//                tvDiscount.visibility = View.VISIBLE
//                tvDiscount.text = it
//            } ?: run { tvDiscount.visibility = View.GONE }

//            tvOfferType.text = if (item.productType != null && item.productType!!.isDineIn()) {
//                context.getString(R.string.dine_in)
//            } else if (item.productType != null && item.productType!!.isPickUp()) {
//                context.getString(R.string.pick_up)
//            } else {
//                ""
//            }

            tvDescriptionSpend.text = item.caption
//            if (!TextUtils.isEmpty(item.caption)) {
//                tvDescriptionSpend.visibility = View.VISIBLE
//                tvDescriptionSpend.text = item.caption
//            } else {
//                tvDescriptionSpend.visibility = View.GONE
//            }

            if (!TextUtils.isEmpty(item.imageUrl)) {
                Glide.with(context).load(item.imageUrl)
                    .apply(
                        RequestOptions().transform(
                            CenterCrop(),
                            RoundedCorners(ROUND_CORNER.toInt())
                        )
                    )
                    .into(imImage)
            }

            root.setOnClickListener { onItemClicked(item) }

//            if (item.optIn) {
            inView.setOnClickListener {
                removeOffer(position, item)
            }
//            } else {
//                inView.setOnClickListener {
//                    val startDate = formatDate2String(item.timeframe?.start)
//                    val endDate = formatDate2String(item.timeframe?.end)
//
//                    if (TextUtils.equals(startDate, endDate) || TextUtils.isEmpty(startDate)) {
//                        addOffer(position, item, startDate)
//
//                        item.optIn = true
//                        notifyDataSetChanged()
//                    } else {
//                        showDatePicker(
//                            { _: DatePicker?, year: Int, monthOfYear: Int, dayOfMonth: Int ->
//                                val month = if ((monthOfYear + 1) < 10) {
//                                    "0${monthOfYear + 1}"
//                                } else {
//                                    "${monthOfYear + 1}"
//                                }
//
//                                val day = if ((dayOfMonth) < 10) {
//                                    "0$dayOfMonth"
//                                } else {
//                                    "$dayOfMonth"
//                                }
//
//                                addOffer(position, item, "$year-$month-$day")
//
//                                item.optIn = true
//                                notifyDataSetChanged()
//                            }, formatDate2Timestamp(item.timeframe?.start), formatDate2Timestamp(item.timeframe?.end)
//                        )
//                    }
//                }
//            }

            inView.offerEnable(true, optOutEnable)
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

    class OfferHolder constructor(vi: View) : RecyclerView.ViewHolder(vi) {
        val root = vi.findViewById<View>(R.id.root)
        val imImage = vi.findViewById<ImageView>(R.id.imOffer)

        //        val tvDiscount = vi.findViewById<TextView>(R.id.tvDiscount)
        val tvTitle = vi.findViewById<TextView>(R.id.tvTitle)
        val tvDescription = vi.findViewById<TextView>(R.id.tvDescription)
        val tvFrom = vi.findViewById<TextView>(R.id.tvFrom)
        val tvTo = vi.findViewById<TextView>(R.id.tvTo)
        val tvDescriptionSpend = vi.findViewById<TextView>(R.id.tvDescriptionSpend)
        val tvOfferType = vi.findViewById<TextView>(R.id.tvOfferType)
        val inView = vi.findViewById<InView>(R.id.inView)
    }
}