package com.tripian.trpcore.ui.companion

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tripian.one.api.companion.model.Companion
import com.tripian.trpcore.R
import com.tripian.trpcore.util.widget.TextView

/**
 * Created by semihozkoroglu on 19.08.2020.
 */
abstract class AdapterCompanion(val context: Context, val items: List<Companion>) : RecyclerView.Adapter<AdapterCompanion.SelectCity>() {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    abstract fun onClickedItem(companion: Companion)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectCity {
        return SelectCity(inflater.inflate(R.layout.item_select_companion, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: SelectCity, position: Int) {
        val item = items[position]

        with(holder) {
            tvName.text = item.name

            itemView.setOnClickListener { onClickedItem(item) }
        }
    }

    class SelectCity(vi: View) : RecyclerView.ViewHolder(vi) {
        val tvName: TextView = vi.findViewById(R.id.tvName)
    }
}