package com.tripian.trpcore.ui.companion

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.tripian.one.api.companion.model.Companion
import com.tripian.trpcore.R
import com.tripian.trpcore.util.widget.CardView
import com.tripian.trpcore.util.widget.ImageView
import com.tripian.trpcore.util.widget.TextView

/**
 * Created by semihozkoroglu on 19.08.2020.
 */
abstract class AdapterSelectCompanion(val context: Context, val items: ArrayList<Companion>) :
    RecyclerView.Adapter<AdapterSelectCompanion.SelectCity>() {

    abstract fun onClickedItem()

    private val inflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    private val selectedItems = HashMap<Int, Boolean>()

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

            if (selectedItems.containsKey(position) && selectedItems[position]!!) {
                imCancel.isVisible = true
                tvName.setTextAppearance(R.style.TextSubhead1)
                cardView.setCardBackgroundColor(
                    ContextCompat.getColor(
                        context,
                        R.color.trp_main_gray_sidebar
                    )
                )
            } else {
                imCancel.isVisible = false
                tvName.setTextAppearance(R.style.TextSubhead1_Regular)
                cardView.setCardBackgroundColor(
                    ContextCompat.getColor(
                        context,
                        R.color.trp_transparent
                    )
                )
            }

            itemView.tag = position
            itemView.setOnClickListener { vi ->
                val pos = vi.tag as Int

                if (selectedItems.containsKey(pos)) {
                    selectedItems[pos] = !selectedItems[pos]!!
                } else {
                    selectedItems[pos] = true
                }

                onClickedItem()

                notifyDataSetChanged()
            }
        }
    }

    fun getSelectedItems(): List<Companion> {
        val companions = ArrayList<Companion>()

        selectedItems.entries.forEach {
            if (it.value && it.key < items.size && it.key != -1) {
                companions.add(items[it.key])
            }
        }

        return companions
    }

    fun setSelectedItems(companions: List<Companion>?) {
        companions?.forEach { c ->
            selectedItems[items.indexOfFirst { it.id == c.id }] = true
        }
    }

    class SelectCity(vi: View) : RecyclerView.ViewHolder(vi) {
        val root: ConstraintLayout = vi.findViewById(R.id.root)
        val tvName: TextView = vi.findViewById(R.id.tvName)
        val cardView: CardView = vi.findViewById(R.id.cardView)
        val imCancel: ImageView = vi.findViewById(R.id.imCancel)
    }
}