package com.tripian.trpcore.ui.timeline.compose.sheets

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.recyclerview.widget.LinearLayoutManager
import com.tripian.one.api.pois.model.PoiCategoryGroup
import com.tripian.trpcore.R
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.BottomSheetPoiFilterBinding
import com.tripian.trpcore.databinding.BottomSheetPoiSortBinding
import com.tripian.trpcore.domain.model.timeline.FilterData
import com.tripian.trpcore.domain.model.timeline.SortOption
import com.tripian.trpcore.ui.timeline.compose.core.BindingHost
import com.tripian.trpcore.ui.timeline.compose.core.TimelineSheet
import com.tripian.trpcore.ui.timeline.poilisting.CategoryFilterAdapter
import com.tripian.trpcore.util.LanguageConst

private fun language(key: String): String = TRPCore.core.miscRepository.getLanguageValueForKey(key)

/**
 * Compose counterpart of the POI SortBottomSheet: popularity or rating.
 * Picking a different option than [current] reports it and closes the sheet.
 */
@Composable
internal fun PoiSortSheet(
    current: SortOption,
    onSelect: (SortOption) -> Unit,
    onDismiss: () -> Unit
) {
    TimelineSheet(onDismissRequest = onDismiss) {
        BindingHost(
            inflate = BottomSheetPoiSortBinding::inflate,
            modifier = Modifier.fillMaxWidth()
        ) { binding, _, _ ->
            binding.tvTitle.text = language(LanguageConst.ADD_PLAN_SORT_BY)
            binding.rbPopularity.text = language(SortOption.POPULARITY.languageKey)
            binding.rbRating.text = language(SortOption.RATING.languageKey)
            when (current) {
                SortOption.RATING -> binding.rbRating.isChecked = true
                else -> binding.rbPopularity.isChecked = true
            }
            binding.ivClose.setOnClickListener { onDismiss() }
            binding.rgSortOptions.setOnCheckedChangeListener { _, checkedId ->
                val selected = when (checkedId) {
                    R.id.rbPopularity -> SortOption.POPULARITY
                    R.id.rbRating -> SortOption.RATING
                    else -> SortOption.DEFAULT
                }
                if (selected != current) {
                    onSelect(selected)
                    onDismiss()
                }
            }
        }
    }
}

/**
 * Compose counterpart of the POI FilterBottomSheet: category checkboxes with
 * Clear and Confirm. Confirm reports the new [FilterData] and closes the sheet.
 */
@Composable
internal fun PoiFilterSheet(
    currentFilter: FilterData,
    categoryGroups: List<PoiCategoryGroup>,
    onApply: (FilterData) -> Unit,
    onDismiss: () -> Unit
) {
    TimelineSheet(onDismissRequest = onDismiss) {
        BindingHost(
            inflate = BottomSheetPoiFilterBinding::inflate,
            modifier = Modifier.fillMaxWidth()
        ) { binding, _, _ ->
            val selectedCategoryIds = currentFilter.selectedCategoryIds.toMutableSet()
            val adapter = CategoryFilterAdapter(
                categoryGroups = categoryGroups,
                selectedCategoryIds = selectedCategoryIds,
                onCategoryToggled = { categoryId, isSelected ->
                    if (isSelected) selectedCategoryIds.add(categoryId) else selectedCategoryIds.remove(categoryId)
                }
            )
            binding.tvTitle.text = language(LanguageConst.ADD_PLAN_FILTERS)
            binding.btnClear.text = language(LanguageConst.ADD_PLAN_CLEAR_SELECTION)
            binding.btnConfirm.text = language(LanguageConst.ADD_PLAN_CONFIRM)
            binding.rvCategories.layoutManager = LinearLayoutManager(binding.root.context)
            binding.rvCategories.adapter = adapter
            binding.ivClose.setOnClickListener { onDismiss() }
            binding.btnClear.setOnClickListener {
                selectedCategoryIds.clear()
                adapter.clearSelections()
            }
            binding.btnConfirm.setOnClickListener {
                onApply(FilterData(selectedCategoryIds = selectedCategoryIds.toList()))
                onDismiss()
            }
        }
    }
}
