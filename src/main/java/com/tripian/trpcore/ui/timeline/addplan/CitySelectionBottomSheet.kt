package com.tripian.trpcore.ui.timeline.addplan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tripian.one.api.cities.model.City
import com.tripian.trpcore.R
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.BottomSheetCitySelectionBinding
import com.tripian.trpcore.databinding.ItemCitySelectionBinding
import com.tripian.trpcore.util.LanguageConst

/**
 * CitySelectionBottomSheet
 * Bottom sheet for selecting a city from the available cities list
 */
class CitySelectionBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCitySelectionBinding? = null
    private val binding get() = _binding!!

    private var cities: List<City> = emptyList()
    private var selectedCityId: Int? = null
    private var onCitySelected: ((City) -> Unit)? = null

    override fun getTheme(): Int = R.style.TrpTimelineBottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetCitySelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupLabels()

        binding.ivClose.setOnClickListener {
            dismiss()
        }

        setupRecyclerView()
    }

    /**
     * Set all label texts using language service
     */
    private fun setupLabels() {
        binding.tvTitle.text = TRPCore.core.miscRepository.getLanguageValueForKey(LanguageConst.ADD_PLAN_SELECT_CITY)
    }

    private fun setupRecyclerView() {
        // Filter unique cities by id
        val uniqueCities = cities.distinctBy { it.id }

        binding.rvCities.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = CityAdapter(uniqueCities, selectedCityId) { city ->
                onCitySelected?.invoke(city)
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "CitySelectionBottomSheet"

        fun newInstance(
            cities: List<City>,
            selectedCity: City?,
            onCitySelected: (City) -> Unit
        ): CitySelectionBottomSheet {
            return CitySelectionBottomSheet().apply {
                this.cities = cities
                this.selectedCityId = selectedCity?.id
                this.onCitySelected = onCitySelected
            }
        }
    }

    /**
     * Adapter for city list
     */
    private inner class CityAdapter(
        private val cities: List<City>,
        private val selectedCityId: Int?,
        private val onItemClick: (City) -> Unit
    ) : RecyclerView.Adapter<CityAdapter.CityViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityViewHolder {
            val binding = ItemCitySelectionBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return CityViewHolder(binding)
        }

        override fun onBindViewHolder(holder: CityViewHolder, position: Int) {
            holder.bind(cities[position])
        }

        override fun getItemCount(): Int = cities.size

        inner class CityViewHolder(
            private val binding: ItemCitySelectionBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(city: City) {
                binding.tvCityName.text = city.name ?: ""

                val isSelected = city.id == selectedCityId
                val context = binding.root.context

                // Set color: selected = primary, unselected = text_primary
                val textColor = if (isSelected) {
                    ContextCompat.getColor(context, R.color.trp_colorPrimary)
                } else {
                    ContextCompat.getColor(context, R.color.trp_text_primary)
                }
                binding.tvCityName.setTextColor(textColor)

                // Set font: selected = semibold, unselected = light
                val fontRes = if (isSelected) R.font.semibold else R.font.light
                binding.tvCityName.typeface = ResourcesCompat.getFont(context, fontRes)

                binding.root.setOnClickListener {
                    onItemClick(city)
                }
            }
        }
    }
}
