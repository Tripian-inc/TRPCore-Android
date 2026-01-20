package com.tripian.trpcore.ui.timeline.addplan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.FrAddPlanSelectDayBinding
import com.tripian.trpcore.domain.model.timeline.AddPlanMode
import com.tripian.trpcore.domain.model.timeline.ManualCategory
import com.tripian.trpcore.ui.timeline.adapter.DayFilterAdapter
import com.tripian.trpcore.util.LanguageConst

/**
 * FRSelectDay
 * Step 1: Day, City, Mode, Manual Category Selection
 * iOS Reference: AddPlanSelectDayVC.swift
 */
class FRSelectDay : Fragment() {

    private var _binding: FrAddPlanSelectDayBinding? = null
    private val binding get() = _binding!!

    // Shared ViewModel from parent
    private val sharedVM: AddPlanContainerVM by lazy {
        ViewModelProvider(requireParentFragment())[AddPlanContainerVM::class.java]
    }

    private var dayFilterAdapter: DayFilterAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FrAddPlanSelectDayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupLabels()
        setupDayFilterRecyclerView()
        setupModeSelection()
        setupManualCategorySelection()
        setupCitySelection()
        observeViewModel()
    }

    /**
     * Set all label texts using language service
     */
    private fun setupLabels() {
        val getLanguage: (String) -> String = { key ->
            TRPCore.core.miscRepository.getLanguageValueForKey(key)
        }

        // Add to Day section
        binding.tvAddToDay.text = getLanguage(LanguageConst.ADD_PLAN_ADD_TO_DAY)

        // City section
        binding.tvCityLabel.text = getLanguage(LanguageConst.ADD_PLAN_CITY)

        // Mode selection section
        binding.tvHowToAdd.text = getLanguage(LanguageConst.ADD_PLAN_HOW_TO_ADD)
        binding.tvSmartTitle.text = getLanguage(LanguageConst.ADD_PLAN_SMART_RECOMMENDATIONS)
        binding.tvSmartDesc.text = getLanguage(LanguageConst.ADD_PLAN_SMART_DESC)
        binding.tvManualTitle.text = getLanguage(LanguageConst.ADD_PLAN_ADD_MANUALLY)
        binding.tvManualDesc.text = getLanguage(LanguageConst.ADD_PLAN_MANUAL_DESC)

        // Manual categories section
        binding.tvSelectCategoriesLabel.text = getLanguage(LanguageConst.ADD_PLAN_SELECT_CATEGORIES)
        binding.tvCatActivities.text = getLanguage(LanguageConst.ADD_PLAN_CAT_MANUAL_ACTIVITIES)
        binding.tvCatPlaces.text = getLanguage(LanguageConst.ADD_PLAN_CAT_MANUAL_PLACES)
        binding.tvCatEatDrink.text = getLanguage(LanguageConst.ADD_PLAN_CAT_MANUAL_EAT_DRINK)
    }

    private fun setupDayFilterRecyclerView() {
        dayFilterAdapter = DayFilterAdapter { position ->
            sharedVM.selectDay(position)
        }
        binding.rvDays.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = dayFilterAdapter
        }
    }

    private fun setupModeSelection() {
        // Smart Recommendations card
        binding.cardSmartRecommendations.setOnClickListener {
            selectMode(AddPlanMode.SMART_RECOMMENDATIONS)
        }

        // Add Manually card
        binding.cardAddManually.setOnClickListener {
            selectMode(AddPlanMode.MANUAL)
        }
    }

    private fun setupManualCategorySelection() {
        binding.cardManualActivities.setOnClickListener {
            selectManualCategory(ManualCategory.ACTIVITIES)
        }

        binding.cardManualPlaces.setOnClickListener {
            selectManualCategory(ManualCategory.PLACES_OF_INTEREST)
        }

        binding.cardManualEatDrink.setOnClickListener {
            selectManualCategory(ManualCategory.EAT_AND_DRINK)
        }
    }

    private fun setupCitySelection() {
        binding.btnCitySelection.setOnClickListener {
            showCitySelectionBottomSheet()
        }
    }

    private fun showCitySelectionBottomSheet() {
        val cities = sharedVM.cities.value ?: return
        if (cities.isEmpty()) return

        val selectedCity = sharedVM.selectedCity.value

        CitySelectionBottomSheet.newInstance(
            cities = cities,
            selectedCity = selectedCity,
            onCitySelected = { city ->
                sharedVM.selectCity(city)
            }
        ).show(childFragmentManager, CitySelectionBottomSheet.TAG)
    }

    private fun selectMode(mode: AddPlanMode) {
        sharedVM.selectMode(mode)
        updateModeSelection(mode)
    }

    private fun selectManualCategory(category: ManualCategory) {
        sharedVM.selectManualCategory(category)
        updateManualCategorySelection(category)
    }

    private fun observeViewModel() {
        // Available days
        sharedVM.availableDays.observe(viewLifecycleOwner) { days ->
            dayFilterAdapter?.setDays(days)
        }

        // Selected day index
        sharedVM.selectedDayIndex.observe(viewLifecycleOwner) { index ->
            dayFilterAdapter?.setSelectedPosition(index)
        }

        // Cities
        sharedVM.cities.observe(viewLifecycleOwner) { cities ->
            // Show city selection only if multiple cities
            binding.llCitySelection.visibility = if (cities.size > 1) View.VISIBLE else View.GONE
        }

        // Selected city
        sharedVM.selectedCity.observe(viewLifecycleOwner) { city ->
            binding.tvSelectedCity.text = city?.name ?: ""
        }

        // Selected mode
        sharedVM.selectedMode.observe(viewLifecycleOwner) { mode ->
            updateModeSelection(mode)
        }

        // Selected manual category
        sharedVM.selectedManualCategory.observe(viewLifecycleOwner) { category ->
            updateManualCategorySelection(category)
        }
    }

    private fun updateModeSelection(mode: AddPlanMode) {
        // Update card selection states
        val smartSelected = mode == AddPlanMode.SMART_RECOMMENDATIONS
        val manualSelected = mode == AddPlanMode.MANUAL

        binding.cardSmartRecommendations.isSelected = smartSelected
        binding.cardAddManually.isSelected = manualSelected

        // Update card borders
        updateCardSelection(binding.cardSmartRecommendations, smartSelected)
        updateCardSelection(binding.cardAddManually, manualSelected)

        // Show/hide manual categories section
        binding.llManualCategories.visibility = if (manualSelected) View.VISIBLE else View.GONE
    }

    private fun updateManualCategorySelection(category: ManualCategory?) {
        // Reset all
        binding.cardManualActivities.isSelected = false
        binding.cardManualPlaces.isSelected = false
        binding.cardManualEatDrink.isSelected = false

        // Select the chosen one
        when (category) {
            ManualCategory.ACTIVITIES -> binding.cardManualActivities.isSelected = true
            ManualCategory.PLACES_OF_INTEREST -> binding.cardManualPlaces.isSelected = true
            ManualCategory.EAT_AND_DRINK -> binding.cardManualEatDrink.isSelected = true
            null -> {}
        }

        // Update borders
        updateCardSelection(binding.cardManualActivities, category == ManualCategory.ACTIVITIES)
        updateCardSelection(binding.cardManualPlaces, category == ManualCategory.PLACES_OF_INTEREST)
        updateCardSelection(binding.cardManualEatDrink, category == ManualCategory.EAT_AND_DRINK)
    }

    private fun updateCardSelection(card: com.google.android.material.card.MaterialCardView, selected: Boolean) {
        if (selected) {
            card.strokeColor = requireContext().getColor(com.tripian.trpcore.R.color.text_primary)
            card.strokeWidth = 2
        } else {
            card.strokeColor = requireContext().getColor(com.tripian.trpcore.R.color.lineWeak)
            card.strokeWidth = 1
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
