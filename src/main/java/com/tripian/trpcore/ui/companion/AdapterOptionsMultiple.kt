package com.tripian.trpcore.ui.companion

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.R
import com.tripian.trpcore.util.widget.ImageView
import com.tripian.trpcore.util.widget.TextView
import com.tripian.one.api.trip.model.Answer
import kotlin.collections.set

/**
 * Created by semihozkoroglu on 19.08.2020.
 */
abstract class AdapterOptionsMultiple constructor(val context: Context, val items: List<Answer>, val answers: List<Int>?) :
    RecyclerView.Adapter<AdapterOptionsMultiple.OptionHolder>() {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
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
        return OptionHolder(inflater.inflate(R.layout.item_select_option, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: OptionHolder, position: Int) {
        val item = items[position]

        with(holder) {
            tvName.text = item.name

            if (selectedItems.containsKey(position) && selectedItems[position]!!) {
                imCheck.setImageResource(R.drawable.ic_check_new)

                rvInnerList.visibility = View.VISIBLE
            } else {
                imCheck.setImageResource(R.drawable.ic_check_empty_new)

                rvInnerList.visibility = View.GONE
            }

            val adapter = object : AdapterOptionsMultiple(context, item.subAnswers ?: arrayListOf(), answers) {
                override fun notified() {
                    this@AdapterOptionsMultiple.notified()
                }
            }

            adapters[position] = adapter

            rvInnerList.adapter = adapter

            if (item.imageId != 0) {
                imImage.visibility = View.VISIBLE
                imImage.setImageResource(item.imageId)
            } else {
                imImage.visibility = View.GONE
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
                    adapter.clearSelection()
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
                    (adapter as AdapterOptionsMultiple).getSelectedItems()
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

    fun clearSelection() {
        selectedItems.entries.forEach {
            it.setValue(false)
        }
    }

    class OptionHolder constructor(vi: View) : RecyclerView.ViewHolder(vi) {
        val imImage: ImageView = vi.findViewById(R.id.imImage)
        val tvName: TextView = vi.findViewById(R.id.tvName)
        val imCheck: ImageView = vi.findViewById(R.id.imCheck)
        val rvInnerList: RecyclerView = vi.findViewById(R.id.rvInnerList)
    }
}