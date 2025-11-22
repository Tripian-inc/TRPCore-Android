package com.tripian.trpcore.ui.createtrip

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.R
import com.tripian.trpcore.util.widget.ImageView
import com.tripian.trpcore.util.widget.TextView
import com.tripian.one.api.trip.model.Answer

/**
 * Created by semihozkoroglu on 19.08.2020.
 */
abstract class AdapterCreateTripBoxAnswer(val context: Context, val items: List<Answer>, val answers: List<Int>?) :
    RecyclerView.Adapter<AdapterCreateTripBoxAnswer.OptionHolder>() {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private var selectedItem = -1

    abstract fun notified()

    init {
        if (answers != null) {
            for (i in items.indices) {
                if (answers.contains(items[i].id)) {
                    selectedItem = i
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionHolder {
        return OptionHolder(inflater.inflate(R.layout.item_select_answer_box, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: OptionHolder, position: Int) {
        val item = items[position]

        with(holder) {
            tvName.text = item.name
            tvDescription.text = item.description

            if (selectedItem == position) {
                rlBackground.setBackgroundResource(R.drawable.bg_oval_light_red_without_line)
                imCheck.setImageResource(R.drawable.ic_check_circle)
            } else {
                rlBackground.setBackgroundResource(R.drawable.bg_oval_transparent)
                imCheck.setImageResource(R.drawable.ic_check_empty_circle)
            }

            itemView.tag = position
            itemView.setOnClickListener { vi ->
                val pos = vi.tag as Int

                if (selectedItem == pos) {
                    selectedItem = -1
                } else {
                    selectedItem = pos
                }

                notifyDataSetChanged()
                notified()
            }
        }
    }

    fun getSelectedItems(): Array<Int> {
        val answers = ArrayList<Int>()

        if (selectedItem != -1) {
            answers.add(items[selectedItem].id ?: 0)
        }

        return answers.toTypedArray()
    }

    class OptionHolder(vi: View) : RecyclerView.ViewHolder(vi) {
        val tvName: TextView = vi.findViewById(R.id.tvName)
        val tvDescription: TextView = vi.findViewById(R.id.tvDescription)
        val rlBackground: RelativeLayout = vi.findViewById(R.id.rlBackground)
        val imCheck: ImageView = vi.findViewById(R.id.imCheck)
    }
}