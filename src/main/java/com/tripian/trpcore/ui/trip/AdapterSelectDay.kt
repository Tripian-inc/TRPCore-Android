package com.tripian.trpcore.ui.trip

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.R
import com.tripian.trpcore.util.widget.ImageView
import com.tripian.trpcore.util.widget.TextView

/**
 * Created by semihozkoroglu on 19.08.2020.
 */
abstract class AdapterSelectDay constructor(val context: Context, val items: List<DayItem>) :
    RecyclerView.Adapter<AdapterSelectDay.SelectDay>() {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    abstract fun onClickedItem(position: Int)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectDay {
        return SelectDay(inflater.inflate(R.layout.item_select_day, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: SelectDay, position: Int) {
        val item = items[position]

        with(holder) {
            tvName.text = item.day

            if (item.selected) {
                imCheck.visibility = View.VISIBLE
            } else {
                imCheck.visibility = View.GONE
            }

            itemView.tag = position
            itemView.setOnClickListener { vi ->
                items.forEach { it.selected = false }
                item.selected = true

                onClickedItem(vi.tag as Int)
                notifyDataSetChanged()
            }
        }
    }

    class SelectDay constructor(vi: View) : RecyclerView.ViewHolder(vi) {
        val tvName: TextView = vi.findViewById(R.id.tvName)
        val imCheck: ImageView = vi.findViewById(R.id.imCheck)
    }
}