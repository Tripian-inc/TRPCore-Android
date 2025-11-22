package com.tripian.trpcore.ui.companion

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.R
import com.tripian.trpcore.domain.model.Pace
import com.tripian.trpcore.util.widget.ImageView
import com.tripian.trpcore.util.widget.TextView

/**
 * Created by semihozkoroglu on 19.08.2020.
 */
abstract class AdapterPace constructor(val context: Context, val items: List<Pace>, var selectedPace: String) : RecyclerView.Adapter<AdapterPace.OptionHolder>() {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    abstract fun onItemSelected(pace: Pace)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionHolder {
        return OptionHolder(inflater.inflate(R.layout.item_select_option, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: OptionHolder, position: Int) {
        val item = items[position]

        with(holder) {
            tvName.text = item.paceName

            if (TextUtils.equals(selectedPace, item.paceSymbol)) {
                imCheck.visibility = View.VISIBLE
            } else {
                imCheck.visibility = View.GONE
            }

            itemView.tag = position
            itemView.setOnClickListener { vi ->
                val pos = vi.tag as Int

                selectedPace = item.paceSymbol

                notifyDataSetChanged()

                onItemSelected(item)
            }
        }
    }

    class OptionHolder constructor(vi: View) : RecyclerView.ViewHolder(vi) {
        val tvName: TextView = vi.findViewById(R.id.tvName)
        val imCheck: ImageView = vi.findViewById(R.id.imCheck)
    }
}