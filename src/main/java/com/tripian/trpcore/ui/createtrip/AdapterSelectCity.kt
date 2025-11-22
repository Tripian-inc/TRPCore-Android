package com.tripian.trpcore.ui.createtrip

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tripian.one.api.cities.model.City
import com.tripian.trpcore.R
import com.tripian.trpcore.domain.model.CitySelect
import com.tripian.trpcore.util.widget.ImageView
import com.tripian.trpcore.util.widget.TextView

/**
 * Created by semihozkoroglu on 19.08.2020.
 */
abstract class AdapterSelectCity(val context: Context, val items: List<CitySelect>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    abstract fun onClickedItem(city: City)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 0) {
            SelectCity(inflater.inflate(R.layout.item_select_city, parent, false))
        } else {
            CityTitle(inflater.inflate(R.layout.item_select_city_title, parent, false))
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (TextUtils.isEmpty(items[position].title)) {
            // city
            return 0
        } else {
            // title
            return 1
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]

        with(holder) {
            if (holder is SelectCity) {
                val city = item.city!!

                holder.tvCityName.text = city.name
                holder.tvCountyName.text = city.country?.name

                itemView.setOnClickListener { onClickedItem(city) }
            } else if (holder is CityTitle) {
                holder.tvTitle.text = item.title
                holder.imIcon.setImageResource(item.imageId)
            }
        }
    }

    class CityTitle(vi: View) : RecyclerView.ViewHolder(vi) {
        val imIcon: ImageView = vi.findViewById(R.id.imIcon)
        val tvTitle: TextView = vi.findViewById(R.id.tvTitle)
    }

    class SelectCity(vi: View) : RecyclerView.ViewHolder(vi) {
        val tvCityName: TextView = vi.findViewById(R.id.tvCityName)
        val tvCountyName: TextView = vi.findViewById(R.id.tvCountyName)
    }
}