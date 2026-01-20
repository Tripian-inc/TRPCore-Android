package com.tripian.trpcore.ui.timeline.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tripian.trpcore.R
import com.tripian.trpcore.databinding.BottomSheetActivityFilterBinding

/**
 * ActivityFilterBottomSheet
 * Bottom sheet for filtering activities by price and duration
 *
 * Features:
 * - Price range slider (Free - 1500€, step 10€)
 * - Duration range slider (0h - 24h, step 30min)
 * - Clear Selection to reset filters
 * - Confirm to apply filters
 * - Persists filter state when reopened
 */
class ActivityFilterBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetActivityFilterBinding? = null
    private val binding get() = _binding!!

    private var currentFilter: ActivityFilterData = ActivityFilterData.default()
    private var currency: String = "EUR"

    private var onFilterConfirmedListener: ((ActivityFilterData) -> Unit)? = null
    private var getLanguageForKey: ((String) -> String)? = null

    override fun getTheme(): Int = R.style.TimelineBottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetActivityFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Restore from arguments
        @Suppress("DEPRECATION")
        arguments?.let { args ->
            (args.getSerializable(ARG_CURRENT_FILTER) as? ActivityFilterData)?.let {
                currentFilter = it
            }
            args.getString(ARG_CURRENCY)?.let {
                currency = it
            }
        }

        setupUI()
        setupSliders()
        setupListeners()
    }

    private fun setupUI() {
        // Set localized texts
        binding.tvTitle.text = getLanguage(LANG_KEY_FILTERS)
        binding.tvPriceLabel.text = getLanguage(LANG_KEY_PRICE)
        binding.tvDurationLabel.text = getLanguage(LANG_KEY_DURATION)
        binding.tvClearSelection.text = getLanguage(LANG_KEY_CLEAR_SELECTION)
        binding.btnConfirm.text = getLanguage(LANG_KEY_CONFIRM)

        // Update range labels
        updatePriceLabels()
        updateDurationLabels()
    }

    private fun setupSliders() {
        // Custom thumb drawable for both sliders
        val thumbDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.bg_slider_thumb)

        // Thumb radius in pixels (12.5dp = 25dp diameter / 2)
        val thumbRadiusPx = (12.5f * resources.displayMetrics.density).toInt()

        // Price slider
        binding.sliderPrice.apply {
            valueFrom = ActivityFilterData.DEFAULT_MIN_PRICE
            valueTo = ActivityFilterData.DEFAULT_MAX_PRICE
            stepSize = ActivityFilterData.PRICE_STEP
            values = listOf(currentFilter.minPrice, currentFilter.maxPrice)

            // Set thumb radius and custom drawable
            thumbRadius = thumbRadiusPx
            thumbDrawable?.let { setCustomThumbDrawable(it) }

            addOnChangeListener { slider, _, _ ->
                val values = slider.values
                currentFilter = currentFilter.copy(
                    minPrice = values[0],
                    maxPrice = values[1]
                )
                updatePriceLabels()
            }
        }

        // Duration slider
        binding.sliderDuration.apply {
            valueFrom = ActivityFilterData.DEFAULT_MIN_DURATION
            valueTo = ActivityFilterData.DEFAULT_MAX_DURATION
            stepSize = ActivityFilterData.DURATION_STEP
            values = listOf(currentFilter.minDuration, currentFilter.maxDuration)

            // Set thumb radius and custom drawable
            thumbRadius = thumbRadiusPx
            thumbDrawable?.let { setCustomThumbDrawable(it) }

            addOnChangeListener { slider, _, _ ->
                val values = slider.values
                currentFilter = currentFilter.copy(
                    minDuration = values[0],
                    maxDuration = values[1]
                )
                updateDurationLabels()
            }
        }
    }

    private fun setupListeners() {
        // Close button
        binding.ivClose.setOnClickListener {
            dismiss()
        }

        // Clear Selection
        binding.tvClearSelection.setOnClickListener {
            resetFilters()
        }

        // Confirm
        binding.btnConfirm.setOnClickListener {
            onFilterConfirmedListener?.invoke(currentFilter)
            dismiss()
        }
    }

    private fun updatePriceLabels() {
        val minPrice = currentFilter.minPrice
        val maxPrice = currentFilter.maxPrice

        // Min price label
        binding.tvPriceMin.text = if (minPrice == 0f) {
            getLanguage(LANG_KEY_FREE)
        } else {
            formatPrice(minPrice)
        }

        // Max price label
        binding.tvPriceMax.text = formatPrice(maxPrice)
    }

    private fun updateDurationLabels() {
        binding.tvDurationMin.text = currentFilter.formatDuration(currentFilter.minDuration)
        binding.tvDurationMax.text = currentFilter.formatDuration(currentFilter.maxDuration)
    }

    private fun formatPrice(price: Float): String {
        val priceInt = price.toInt()
        val symbol = when (currency.uppercase()) {
            "EUR" -> "€"
            "USD" -> "$"
            "GBP" -> "£"
            "TRY" -> "₺"
            else -> currency
        }

        // EUR: symbol after, others: symbol before
        return if (currency.uppercase() == "EUR") {
            "$priceInt$symbol"
        } else {
            "$symbol$priceInt"
        }
    }

    private fun resetFilters() {
        currentFilter = ActivityFilterData.default()

        // Reset sliders
        binding.sliderPrice.values = listOf(
            ActivityFilterData.DEFAULT_MIN_PRICE,
            ActivityFilterData.DEFAULT_MAX_PRICE
        )
        binding.sliderDuration.values = listOf(
            ActivityFilterData.DEFAULT_MIN_DURATION,
            ActivityFilterData.DEFAULT_MAX_DURATION
        )

        // Update labels
        updatePriceLabels()
        updateDurationLabels()
    }

    private fun getLanguage(key: String): String {
        val result = getLanguageForKey?.invoke(key)
        if (!result.isNullOrEmpty()) return result

        return when (key) {
            LANG_KEY_FILTERS -> "Filters"
            LANG_KEY_PRICE -> "Price"
            LANG_KEY_DURATION -> "Duration"
            LANG_KEY_CLEAR_SELECTION -> "Clear Selection"
            LANG_KEY_CONFIRM -> "Confirm"
            LANG_KEY_FREE -> "Free"
            else -> key
        }
    }

    fun setOnFilterConfirmedListener(listener: (ActivityFilterData) -> Unit) {
        onFilterConfirmedListener = listener
    }

    fun setLanguageProvider(provider: (String) -> String) {
        getLanguageForKey = provider
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ActivityFilterBottomSheet"

        private const val ARG_CURRENT_FILTER = "current_filter"
        private const val ARG_CURRENCY = "currency"

        // Language keys - matching LanguageConsts
        private const val LANG_KEY_FILTERS = "addPlan.button.filters"
        private const val LANG_KEY_PRICE = "addPlan.filter.price"
        private const val LANG_KEY_DURATION = "addPlan.filter.duration"
        private const val LANG_KEY_CLEAR_SELECTION = "addPlan.button.clearSelection"
        private const val LANG_KEY_CONFIRM = "confirm"
        private const val LANG_KEY_FREE = "addPlan.filter.free"

        fun newInstance(
            currentFilter: ActivityFilterData,
            currency: String
        ): ActivityFilterBottomSheet {
            return ActivityFilterBottomSheet().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_CURRENT_FILTER, currentFilter)
                    putString(ARG_CURRENCY, currency)
                }
            }
        }
    }
}
