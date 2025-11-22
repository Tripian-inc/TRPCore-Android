package com.tripian.trpcore.ui.common

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tripian.one.api.pois.model.PoiCategory
import com.tripian.one.api.pois.model.PoiCategoryGroup
import com.tripian.trpcore.R
import com.tripian.trpcore.util.widget.ImageView
import com.tripian.trpcore.util.widget.TextView

/**
 * Created by semihozkoroglu on 19.08.2020.
 */
abstract class AdapterPoiCategories(
    val context: Context,
    val items: List<PoiCategoryGroup>,
    val selectedCategories: List<PoiCategory>?
) :
    RecyclerView.Adapter<AdapterPoiCategories.OptionHolder>() {

    private val inflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private var selectedItems: MutableList<PoiCategory> = arrayListOf()
    private var selectedAllCategories: MutableList<PoiCategoryGroup> = arrayListOf()
    private val adapters = HashMap<Int, RecyclerView.Adapter<*>>()

    init {
        selectedItems = selectedCategories?.toMutableList() ?: mutableListOf()
        checkSelectedCategoriesForInit()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionHolder {
        return OptionHolder(inflater.inflate(R.layout.item_select_poi_category, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: OptionHolder, position: Int) {
        val item = items[position]

        with(holder) {
            tvName.text = item.name

            if (checkAllCategoriesSelected(item)) {
                imCheck.setImageResource(R.drawable.ic_check_new)
                tvName.setTextAppearance(R.style.Medium_16_Black)
            } else {
                imCheck.setImageResource(R.drawable.ic_check_empty_new)
                tvName.setTextAppearance(R.style.Regular_16_DarkGray)
            }

            if (adapters.containsKey(position)) {
                rvInnerList.adapter = adapters[position]
            } else {
                item.categories?.let { categories ->
                    val adapter = object : AdapterPoiCategoriesSub(
                        context = context,
                        items = categories,
                        selectedItems = selectedItems
                    ) {
                        override fun itemClicked(item: PoiCategory) {
                            subItemSelected(item)
                        }
                    }

                    adapters[position] = adapter
                    rvInnerList.adapter = adapter
                }
            }

            itemView.tag = item.name
            itemView.setOnClickListener { vi ->
                val catName = vi.tag as? String
                val category = items.firstOrNull { it.name == catName }

                if (category?.categories != null) {
                    if (selectedAllCategories.contains(category)) {
                        selectedItems.removeAll(category.categories!!)
                        selectedAllCategories.remove(category)
                    } else {
                        selectedItems.addAll(category.categories!!)
                        selectedAllCategories.add(category)
                    }
                }

                notifyItemChanged(position)
//                selectedItemIds(getSelectedItems())
            }
        }
    }

    fun getSelectedItems(): List<PoiCategory> {
        return selectedItems
    }

    private fun subItemSelected(item: PoiCategory) {
        val isSelected: Boolean
        if (selectedItems.contains(item)) {
            selectedItems.remove(item)
            isSelected = false
        } else {
            selectedItems.add(item)
            isSelected = true
        }
        setPoiCategoryGroupSelectedFromItem(item, isSelected)
    }

    private fun setPoiCategoryGroupSelectedFromItem(item: PoiCategory, isSelected: Boolean) {
        val group = items.first { it.categories!!.contains(item) }
        if (isSelected.not()) {
            selectedAllCategories.remove(group)
        } else {
            if (checkAllCategoriesSelected(group)) {
                selectedAllCategories.add(group)
            }
        }
        notifyDataSetChanged()
    }

    private fun checkSelectedCategoriesForInit() {
        items.forEach {
            if (checkAllCategoriesSelected(it)) {
                selectedAllCategories.add(it)
            }
        }
    }

    private fun checkAllCategoriesSelected(group: PoiCategoryGroup): Boolean {
        return selectedItems.containsAll(group.categories ?: listOf())
    }

    class OptionHolder(vi: View) : RecyclerView.ViewHolder(vi) {
        val tvName: TextView = vi.findViewById(R.id.tvName)
        val imCheck: ImageView = vi.findViewById(R.id.imCheck)
        val rvInnerList: RecyclerView = vi.findViewById(R.id.rvInnerList)
    }
}