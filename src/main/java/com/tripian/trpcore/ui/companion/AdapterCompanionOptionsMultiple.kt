package com.tripian.trpcore.ui.companion

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.R
import com.tripian.one.api.trip.model.Answer
import com.tripian.trpcore.util.widget.CheckBoxView
import kotlin.collections.set

/**
 * Created by semihozkoroglu on 19.08.2020.
 */
abstract class AdapterCompanionOptionsMultiple constructor(val context: Context, val items: List<Answer>, val answers: List<Int>?) :
    RecyclerView.Adapter<AdapterCompanionOptionsMultiple.OptionHolder>() {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val selectedItems = HashMap<Int, Boolean>()

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
        return OptionHolder(inflater.inflate(R.layout.item_select_option_check_view, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: OptionHolder, position: Int) {
        val item = items[position]

        with(holder) {
            tvName.text = item.name

            tvName.check(selectedItems.containsKey(position) && selectedItems[position]!!, true)

            tvName.tag = position
            tvName.setOnCheckListener {
                val pos = tvName.tag as Int

                if (selectedItems.containsKey(pos)) {
                    selectedItems[pos] = !selectedItems[pos]!!
                } else {
                    selectedItems[pos] = true
                }

                notifyDataSetChanged()
                notified()
            }
        }
    }

    fun getSelectedItems(): Array<Int> {
        val answers = ArrayList<Int>()

        selectedItems.entries.forEach {
            if (it.value && it.key < items.size && it.key != -1) {
                answers.add(items[it.key].id ?: 0)
            }
        }

        return answers.toTypedArray()
    }

    class OptionHolder constructor(vi: View) : RecyclerView.ViewHolder(vi) {
        val tvName: CheckBoxView = vi.findViewById(R.id.tvName)
    }
}