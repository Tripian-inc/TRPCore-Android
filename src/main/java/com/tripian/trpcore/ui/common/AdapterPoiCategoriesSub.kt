package com.tripian.trpcore.ui.common

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tripian.one.api.pois.model.PoiCategory
import com.tripian.trpcore.R
import com.tripian.trpcore.util.widget.ImageView
import com.tripian.trpcore.util.widget.TextView

/**
 * Created by semihozkoroglu on 19.08.2020.
 */
abstract class AdapterPoiCategoriesSub(
    val context: Context,
    val items: List<PoiCategory>,
    val selectedItems: List<PoiCategory>
) :
    RecyclerView.Adapter<AdapterPoiCategoriesSub.PoiCategorySubHolder>() {

    private val inflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    abstract fun itemClicked(item: PoiCategory)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PoiCategorySubHolder {
        return PoiCategorySubHolder(
            inflater.inflate(
                R.layout.item_select_poi_category_sub,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: PoiCategorySubHolder, position: Int) {
        val item = items[position]

        with(holder) {
            tvName.text = item.name

            if (selectedItems.contains(item)) {
                imCheck.setImageResource(R.drawable.ic_check_new)
                tvName.setTextAppearance(R.style.Medium_14_Black)
            } else {
                imCheck.setImageResource(R.drawable.ic_check_empty_new)
                tvName.setTextAppearance(R.style.Regular_14_DarkGray)
            }

            itemView.tag = position
            itemView.setOnClickListener {
                itemClicked(items[position])
                notifyItemChanged(position)
            }
        }
    }

    class PoiCategorySubHolder(vi: View) : RecyclerView.ViewHolder(vi) {
        val tvName: TextView = vi.findViewById(R.id.tvName)
        val imCheck: ImageView = vi.findViewById(R.id.imCheck)
    }
}