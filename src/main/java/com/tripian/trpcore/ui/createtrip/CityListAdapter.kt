package com.tripian.trpcore.ui.createtrip

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tripian.one.api.cities.model.City
import com.tripian.trpcore.databinding.ItemCityDestinationBinding

/**
 * Adapter for the "All destinations" city list on [ACCitySelection]. Single
 * selection — the currently selected city id is passed in via [submitSelection]
 * so the checked row shows a tick.
 */
class CityListAdapter(
    private val onCityClicked: (City) -> Unit
) : ListAdapter<City, CityListAdapter.CityViewHolder>(DiffCallback()) {

    private var selectedCityId: Int? = null

    /** Re-render with a new selection (and optionally a new list). */
    fun submitSelection(cities: List<City>, selectedId: Int?) {
        selectedCityId = selectedId
        submitList(cities)
        notifyDataSetChanged()
    }

    fun setSelected(selectedId: Int?) {
        selectedCityId = selectedId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityViewHolder {
        val binding = ItemCityDestinationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CityViewHolder, position: Int) =
        holder.bind(getItem(position))

    inner class CityViewHolder(
        private val binding: ItemCityDestinationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(city: City) {
            binding.tvCityName.text = city.name ?: ""
            val country = city.country?.name
            binding.tvCountry.text = country ?: ""
            binding.tvCountry.visibility = if (country.isNullOrBlank()) View.GONE else View.VISIBLE
            binding.ivCheck.visibility = if (city.id == selectedCityId) View.VISIBLE else View.GONE
            binding.root.setOnClickListener { onCityClicked(city) }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<City>() {
        override fun areItemsTheSame(oldItem: City, newItem: City) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: City, newItem: City) =
            oldItem.id == newItem.id && oldItem.name == newItem.name
    }
}
