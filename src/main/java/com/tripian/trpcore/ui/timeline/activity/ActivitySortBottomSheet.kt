package com.tripian.trpcore.ui.timeline.activity

import android.os.Bundle
import android.view.View
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseSimpleBottomSheet
import com.tripian.trpcore.databinding.BottomSheetActivitySortBinding
import com.tripian.trpcore.domain.model.timeline.SortOption
import com.tripian.trpcore.util.LanguageConst

/**
 * Bottom sheet for sorting Activity list
 * iOS Reference: ActivitySortVC
 */
class ActivitySortBottomSheet : BaseSimpleBottomSheet<BottomSheetActivitySortBinding>(
    BottomSheetActivitySortBinding::inflate
) {

    private var currentSort: SortOption = SortOption.DEFAULT
    private var onSortSelected: ((SortOption) -> Unit)? = null
    private var getLanguage: ((String) -> String)? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let { args ->
            val sortName = args.getString(ARG_SORT)
            currentSort = sortName?.let {
                try { SortOption.valueOf(it) } catch (e: Exception) { SortOption.DEFAULT }
            } ?: SortOption.DEFAULT
        }

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        updateTexts()

        when (currentSort) {
            SortOption.POPULARITY -> binding.rbPopularity.isChecked = true
            SortOption.RATING -> binding.rbRating.isChecked = true
            SortOption.PRICE_LOW_TO_HIGH -> binding.rbPriceLowToHigh.isChecked = true
            SortOption.DURATION_SHORT_TO_LONG -> binding.rbDurationShortToLong.isChecked = true
            SortOption.DURATION_LONG_TO_SHORT -> binding.rbDurationLongToShort.isChecked = true
            else -> {}
        }
    }

    private fun setupListeners() {
        binding.ivClose.setOnClickListener {
            dismiss()
        }

        binding.rgSortOptions.setOnCheckedChangeListener { _, checkedId ->
            val selectedSort = when (checkedId) {
                R.id.rbPopularity -> SortOption.POPULARITY
                R.id.rbRating -> SortOption.RATING
                R.id.rbPriceLowToHigh -> SortOption.PRICE_LOW_TO_HIGH
                R.id.rbDurationShortToLong -> SortOption.DURATION_SHORT_TO_LONG
                R.id.rbDurationLongToShort -> SortOption.DURATION_LONG_TO_SHORT
                else -> SortOption.DEFAULT
            }

            if (selectedSort != currentSort) {
                currentSort = selectedSort
                onSortSelected?.invoke(selectedSort)
                dismiss()
            }
        }
    }

    private fun getLanguageText(key: String, fallback: String = key): String {
        val result = getLanguage?.invoke(key)
        return if (result.isNullOrEmpty()) fallback else result
    }

    fun setOnSortSelectedListener(listener: (SortOption) -> Unit) {
        onSortSelected = listener
    }

    fun setLanguageProvider(provider: (String) -> String) {
        getLanguage = provider
        if (view != null) {
            updateTexts()
        }
    }

    private fun updateTexts() {
        binding.tvTitle.text = getLanguageText(LanguageConst.ADD_PLAN_SORT_BY, "Sort by")
        binding.rbPopularity.text = getLanguageText(SortOption.POPULARITY.languageKey, "Popularity")
        binding.rbRating.text = getLanguageText(SortOption.RATING.languageKey, "Rating")
        binding.rbPriceLowToHigh.text = getLanguageText(SortOption.PRICE_LOW_TO_HIGH.languageKey, "Price: Low to High")
        binding.rbDurationShortToLong.text = getLanguageText(SortOption.DURATION_SHORT_TO_LONG.languageKey, "Duration: Short to Long")
        binding.rbDurationLongToShort.text = getLanguageText(SortOption.DURATION_LONG_TO_SHORT.languageKey, "Duration: Long to Short")
    }

    companion object {
        const val TAG = "ActivitySortBottomSheet"
        private const val ARG_SORT = "sort"

        fun newInstance(
            currentSort: SortOption = SortOption.DEFAULT
        ): ActivitySortBottomSheet {
            return ActivitySortBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_SORT, currentSort.name)
                }
            }
        }
    }
}
