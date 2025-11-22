package com.tripian.trpcore.ui.createtrip

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tripian.one.api.trip.model.Answer
import com.tripian.trpcore.R
import com.tripian.trpcore.util.widget.TextView

/**
 * Created by semihozkoroglu on 19.08.2020.
 */
abstract class AdapterSelectAnswerBottom(val context: Context, val items: List<Answer>) :
    RecyclerView.Adapter<AdapterSelectAnswerBottom.SelectAnswer>() {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    abstract fun onClickedItem(answer: Answer)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectAnswer {
        return SelectAnswer(inflater.inflate(R.layout.item_select_answer_bottom, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: SelectAnswer, position: Int) {
        val item = items[position]

        with(holder) {
            tvName.text = item.name
            tvName.setOnClickListener { onClickedItem(item) }
        }
    }

    class SelectAnswer(vi: View) : RecyclerView.ViewHolder(vi) {
        val tvName: TextView = vi.findViewById(R.id.tvName)
    }
}