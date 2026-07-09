package com.tripian.trpcore.ui.timeline.activity

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseSimpleBottomSheet
import com.tripian.trpcore.databinding.BottomSheetActivityFilterBinding
import com.tripian.trpcore.util.FormatUtils
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Bottom sheet for filtering activities with price and duration range sliders.
 * Persists filter state when reopened.
 */
class ActivityFilterBottomSheet : BaseSimpleBottomSheet<BottomSheetActivityFilterBinding>(
    BottomSheetActivityFilterBinding::inflate
) {

    private var currentFilter: ActivityFilterData = ActivityFilterData.default()
    private var currency: String = "EUR"

    /** Optional facet-driven bounds; when null the sliders fall back to ActivityFilterData.DEFAULT_*. */
    private var minPriceBound: Float? = null
    private var maxPriceBound: Float? = null
    private var minDurationBound: Float? = null
    private var maxDurationBound: Float? = null

    private var onFilterConfirmedListener: ((ActivityFilterData) -> Unit)? = null
    private var getLanguageForKey: ((String) -> String)? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        @Suppress("DEPRECATION")
        arguments?.let { args ->
            (args.getSerializable(ARG_CURRENT_FILTER) as? ActivityFilterData)?.let {
                currentFilter = it
            }
            args.getString(ARG_CURRENCY)?.let {
                currency = it
            }
            if (args.containsKey(ARG_PRICE_MIN_BOUND)) {
                minPriceBound = args.getFloat(ARG_PRICE_MIN_BOUND)
            }
            if (args.containsKey(ARG_PRICE_MAX_BOUND)) {
                maxPriceBound = args.getFloat(ARG_PRICE_MAX_BOUND)
            }
            if (args.containsKey(ARG_DURATION_MIN_BOUND)) {
                minDurationBound = args.getFloat(ARG_DURATION_MIN_BOUND)
            }
            if (args.containsKey(ARG_DURATION_MAX_BOUND)) {
                maxDurationBound = args.getFloat(ARG_DURATION_MAX_BOUND)
            }
        }

        setupUI()
        setupSliders()
        setupListeners()
    }

    private fun setupUI() {
        binding.tvTitle.text = getLanguage(LANG_KEY_FILTERS)
        binding.tvPriceLabel.text = getLanguage(LANG_KEY_PRICE)
        binding.tvDurationLabel.text = getLanguage(LANG_KEY_DURATION)
        binding.tvClearSelection.text = getLanguage(LANG_KEY_CLEAR_SELECTION)
        binding.btnConfirm.text = getLanguage(LANG_KEY_CONFIRM)

        updatePriceLabels()
        updateDurationLabels()
    }

    /**
     * Facet bounds are snapped to the step grid: Material Slider throws when
     * (valueTo - valueFrom) isn't an exact multiple of stepSize.
     */
    private fun setupSliders() {
        val thumbDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.trp_bg_slider_thumb)

        val thumbRadiusPx = (12.5f * resources.displayMetrics.density).toInt()

        val priceFromRaw = minPriceBound ?: ActivityFilterData.DEFAULT_MIN_PRICE
        val priceToRaw = (maxPriceBound ?: ActivityFilterData.DEFAULT_MAX_PRICE)
            .coerceAtLeast(priceFromRaw + ActivityFilterData.PRICE_STEP)
        val priceFrom = snapDown(priceFromRaw, ActivityFilterData.PRICE_STEP)
        val priceTo = snapUp(priceToRaw, ActivityFilterData.PRICE_STEP)
            .coerceAtLeast(priceFrom + ActivityFilterData.PRICE_STEP)
        binding.sliderPrice.apply {
            valueFrom = priceFrom
            valueTo = priceTo
            stepSize = ActivityFilterData.PRICE_STEP
            values = listOf(
                currentFilter.minPrice.coerceIn(priceFrom, priceTo),
                currentFilter.maxPrice.coerceIn(priceFrom, priceTo)
            )

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

        val durationFromRaw = minDurationBound ?: ActivityFilterData.DEFAULT_MIN_DURATION
        val durationToRaw = (maxDurationBound ?: ActivityFilterData.DEFAULT_MAX_DURATION)
            .coerceAtLeast(durationFromRaw + ActivityFilterData.DURATION_STEP)
        val durationFrom = snapDown(durationFromRaw, ActivityFilterData.DURATION_STEP)
        val durationTo = snapUp(durationToRaw, ActivityFilterData.DURATION_STEP)
            .coerceAtLeast(durationFrom + ActivityFilterData.DURATION_STEP)
        binding.sliderDuration.apply {
            valueFrom = durationFrom
            valueTo = durationTo
            stepSize = ActivityFilterData.DURATION_STEP
            values = listOf(
                currentFilter.minDuration.coerceIn(durationFrom, durationTo),
                currentFilter.maxDuration.coerceIn(durationFrom, durationTo)
            )

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

    /** Snap a value down to the nearest multiple of [step]. */
    private fun snapDown(value: Float, step: Float): Float =
        if (step <= 0f) value else floor(value / step) * step

    /** Snap a value up to the nearest multiple of [step]. */
    private fun snapUp(value: Float, step: Float): Float =
        if (step <= 0f) value else ceil(value / step) * step

    private fun setupListeners() {
        binding.ivClose.setOnClickListener {
            dismiss()
        }

        binding.tvClearSelection.setOnClickListener {
            resetFilters()
        }

        binding.btnConfirm.setOnClickListener {
            onFilterConfirmedListener?.invoke(currentFilter)
            dismiss()
        }
    }

    private fun updatePriceLabels() {
        val minPrice = currentFilter.minPrice
        val maxPrice = currentFilter.maxPrice

        binding.tvPriceMin.text = if (minPrice == 0f) {
            getLanguage(LANG_KEY_FREE)
        } else {
            formatPrice(minPrice)
        }

        binding.tvPriceMax.text = formatPrice(maxPrice)
    }

    private fun updateDurationLabels() {
        binding.tvDurationMin.text = FormatUtils.formatDuration(currentFilter.minDuration)
        binding.tvDurationMax.text = FormatUtils.formatDuration(currentFilter.maxDuration)
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

        return if (currency.uppercase() == "EUR") {
            "$priceInt$symbol"
        } else {
            "$symbol$priceInt"
        }
    }

    private fun resetFilters() {
        currentFilter = ActivityFilterData.default()

        binding.sliderPrice.values = listOf(
            ActivityFilterData.DEFAULT_MIN_PRICE,
            ActivityFilterData.DEFAULT_MAX_PRICE
        )
        binding.sliderDuration.values = listOf(
            ActivityFilterData.DEFAULT_MIN_DURATION,
            ActivityFilterData.DEFAULT_MAX_DURATION
        )

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

    companion object {
        const val TAG = "ActivityFilterBottomSheet"

        private const val ARG_CURRENT_FILTER = "current_filter"
        private const val ARG_CURRENCY = "currency"

        private const val LANG_KEY_FILTERS = "addPlan.button.filters"
        private const val LANG_KEY_PRICE = "addPlan.filter.price"
        private const val LANG_KEY_DURATION = "addPlan.filter.duration"
        private const val LANG_KEY_CLEAR_SELECTION = "addPlan.button.clearSelection"
        private const val LANG_KEY_CONFIRM = "confirm"
        private const val LANG_KEY_FREE = "addPlan.filter.free"

        fun newInstance(
            currentFilter: ActivityFilterData,
            currency: String,
            minPriceBound: Float? = null,
            maxPriceBound: Float? = null,
            minDurationBound: Float? = null,
            maxDurationBound: Float? = null
        ): ActivityFilterBottomSheet {
            return ActivityFilterBottomSheet().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_CURRENT_FILTER, currentFilter)
                    putString(ARG_CURRENCY, currency)
                    minPriceBound?.let { putFloat(ARG_PRICE_MIN_BOUND, it) }
                    maxPriceBound?.let { putFloat(ARG_PRICE_MAX_BOUND, it) }
                    minDurationBound?.let { putFloat(ARG_DURATION_MIN_BOUND, it) }
                    maxDurationBound?.let { putFloat(ARG_DURATION_MAX_BOUND, it) }
                }
            }
        }

        private const val ARG_PRICE_MIN_BOUND = "filter_price_min_bound"
        private const val ARG_PRICE_MAX_BOUND = "filter_price_max_bound"
        private const val ARG_DURATION_MIN_BOUND = "filter_duration_min_bound"
        private const val ARG_DURATION_MAX_BOUND = "filter_duration_max_bound"
    }
}
