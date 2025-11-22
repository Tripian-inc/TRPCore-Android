package com.tripian.trpcore.ui.createtrip

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.tripian.one.api.trip.model.Answer
import com.tripian.trpcore.R
import com.tripian.trpcore.util.widget.ImageView
import com.tripian.trpcore.util.widget.TextView
import kotlin.collections.set

/**
 * Created by semihozkoroglu on 19.08.2020.
 */
abstract class AdapterCreateTripAnswerMultiple(
    val context: Context,
    val items: List<Answer>,
    val answers: List<Int>?,
    val isSubAnswers: Boolean = false
) :
    RecyclerView.Adapter<AdapterCreateTripAnswerMultiple.OptionHolder>() {

    private val inflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val selectedItems = HashMap<Int, Boolean>()
    private val adapters = HashMap<Int, RecyclerView.Adapter<*>>()

    abstract fun notified()

    init {
        if (answers != null) {
            for (i in items.indices) {
                if (answers.contains(items[i].id)) {
                    selectedItems[i] = true
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionHolder {
        return OptionHolder(inflater.inflate(R.layout.item_select_answer_multiple, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: OptionHolder, position: Int) {
        val item = items[position]

        with(holder) {
            tvName.text = item.name

            imArrow.isVisible = !item.subAnswers.isNullOrEmpty()

            if (selectedItems.containsKey(position) && selectedItems[position]!!) {
                imCheck.setImageResource(R.drawable.ic_check_new)
                imArrow.setImageResource(R.drawable.ic_arrow_up)
                if (isSubAnswers) {
                    tvName.setTextAppearance(R.style.Medium_14_Black)
                } else {
                    tvName.setTextAppearance(R.style.Medium_16_Black)
                }
                rvInnerList.visibility = View.VISIBLE
            } else {
                imCheck.setImageResource(R.drawable.ic_check_empty_new)
                imArrow.setImageResource(R.drawable.ic_arrow_down)
                if (isSubAnswers) {
                    tvName.setTextAppearance(R.style.Regular_14_DarkGray)
                } else {
                    tvName.setTextAppearance(R.style.Regular_16_DarkGray)
                }
                rvInnerList.visibility = View.GONE
            }

            if (adapters.containsKey(position)) {
                rvInnerList.adapter = adapters[position]
            } else {
                val adapter = object : AdapterCreateTripAnswerMultiple(
                    context,
                    item.subAnswers ?: arrayListOf(),
                    answers,
                    isSubAnswers = true
                ) {
                    override fun notified() {
                        this@AdapterCreateTripAnswerMultiple.notified()
                    }
                }

                adapters[position] = adapter

                rvInnerList.adapter = adapter
            }

            itemView.tag = position
            itemView.setOnClickListener { vi ->
                val pos = vi.tag as Int

                if (selectedItems.containsKey(pos)) {
                    selectedItems[pos] = !selectedItems[pos]!!
                } else {
                    selectedItems[pos] = true
                }

                if (!selectedItems[pos]!!) {
                    (adapters[position] as AdapterCreateTripAnswerMultiple).clearSelection()
                }

                notifyDataSetChanged()
                notified()
            }
        }
    }

    fun getSelectedItems(): Array<Int> {
        val answers = ArrayList<Int>()

        adapters.entries.forEach {
            val adapter = adapters[it.key]

            if (adapter != null) {
                answers.addAll(
                    (adapter as AdapterCreateTripAnswerMultiple).getSelectedItems()
                )
            }
        }

        selectedItems.entries.forEach {
            if (it.value && it.key < items.size && it.key != -1) {
                answers.add(items[it.key].id ?: 0)
            }
        }

        return answers.toTypedArray()
    }

    private fun clearSelection() {
        selectedItems.entries.forEach {
            it.setValue(false)
        }
    }

    class OptionHolder(vi: View) : RecyclerView.ViewHolder(vi) {
        val imArrow: ImageView = vi.findViewById(R.id.imArrow)
        val tvName: TextView = vi.findViewById(R.id.tvName)
        val imCheck: ImageView = vi.findViewById(R.id.imCheck)
        val rvInnerList: RecyclerView = vi.findViewById(R.id.rvInnerList)
    }
}