package com.tripian.trpcore.ui.timeline.poilisting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tripian.trpcore.R
import com.tripian.trpcore.databinding.BottomSheetPoiSortBinding
import com.tripian.trpcore.domain.model.timeline.SortOption
import com.tripian.trpcore.util.LanguageConst

/**
 * SortBottomSheet
 * Bottom sheet for sorting POI list
 * iOS Reference: POISortVC
 */
class SortBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetPoiSortBinding? = null
    private val binding get() = _binding!!

    private var currentSort: SortOption = SortOption.DEFAULT
    private var onSortSelected: ((SortOption) -> Unit)? = null
    private var getLanguage: ((String) -> String)? = null

    override fun getTheme(): Int = R.style.TimelineBottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetPoiSortBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Restore initial values from arguments
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
        // Set localized texts
        binding.tvTitle.text = getLanguageText(LanguageConst.ADD_PLAN_SORT_BY)

        // Set radio button texts (only Popularity and Rating for POI listing)
        binding.rbPopularity.text = getLanguageText(SortOption.POPULARITY.languageKey)
        binding.rbRating.text = getLanguageText(SortOption.RATING.languageKey)

        // Set current selection
        when (currentSort) {
            SortOption.POPULARITY -> binding.rbPopularity.isChecked = true
            SortOption.RATING -> binding.rbRating.isChecked = true
            else -> binding.rbPopularity.isChecked = true // Default to popularity for unsupported options
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
                else -> SortOption.DEFAULT
            }

            if (selectedSort != currentSort) {
                currentSort = selectedSort
                onSortSelected?.invoke(selectedSort)
                dismiss()
            }
        }
    }

    private fun getLanguageText(key: String): String {
        return getLanguage?.invoke(key) ?: key
    }

    fun setOnSortSelectedListener(listener: (SortOption) -> Unit) {
        onSortSelected = listener
    }

    fun setLanguageProvider(provider: (String) -> String) {
        getLanguage = provider
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SortBottomSheet"
        private const val ARG_SORT = "sort"

        fun newInstance(
            currentSort: SortOption = SortOption.DEFAULT
        ): SortBottomSheet {
            return SortBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_SORT, currentSort.name)
                }
            }
        }
    }
}
