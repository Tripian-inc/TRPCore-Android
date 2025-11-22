package com.tripian.trpcore.ui.createtrip

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.R
import com.tripian.trpcore.domain.model.PlaceAutocomplete
import com.tripian.trpcore.util.widget.TextView

/**
 * Created by semihozkoroglu on 19.08.2020.
 */
abstract class AdapterSelectPlace(val context: Context, val items: List<PlaceAutocomplete>) : RecyclerView.Adapter<AdapterSelectPlace.SelectCity>() {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    abstract fun onClickedItem(place: PlaceAutocomplete)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectCity {
        return SelectCity(inflater.inflate(R.layout.item_select_place, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: SelectCity, position: Int) {
        val item = items[position]

        with(holder) {
            tvArea.text = item.area
            tvAddress.text = item.address

            itemView.setOnClickListener { onClickedItem(item) }
        }
    }

    class SelectCity(vi: View) : RecyclerView.ViewHolder(vi) {
        val tvArea: TextView = vi.findViewById(R.id.tvArea)
        val tvAddress: TextView = vi.findViewById(R.id.tvAddress)
    }
}