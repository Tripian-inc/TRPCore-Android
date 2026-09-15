package com.tripian.trpcore.ui.timeline.compose.sheets

import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.tripian.trpcore.R
import com.tripian.trpcore.databinding.BottomSheetActivityFilterBinding
import com.tripian.trpcore.databinding.BottomSheetActivitySortBinding
import com.tripian.trpcore.domain.model.timeline.SortOption
import com.tripian.trpcore.ui.timeline.activity.ActivityFilterData
import com.tripian.trpcore.ui.timeline.compose.core.BindingHost
import com.tripian.trpcore.ui.timeline.compose.core.TimelineSheet
import com.tripian.trpcore.util.FormatUtils
import com.tripian.trpcore.util.LanguageConst
import kotlin.math.ceil
import kotlin.math.floor

private const val SLIDER_THUMB_RADIUS_DP = 12.5f

/**
 * Compose counterpart of ActivitySortBottomSheet: radio list of sort options;
 * picking a different one reports it and closes the sheet.
 */
@Composable
internal fun ActivitySortSheet(
    currentSort: SortOption,
    getLanguage: (String) -> String,
    onSortSelected: (SortOption) -> Unit,
    onDismiss: () -> Unit
) {
    TimelineSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            BindingHost(
                inflate = BottomSheetActivitySortBinding::inflate,
                modifier = Modifier.fillMaxWidth()
            ) { binding, _, _ ->
                binding.root.hideLayoutDragHandle()
                binding.applyTexts(getLanguage)
                binding.checkCurrent(currentSort)
                binding.ivClose.setOnClickListener { onDismiss() }
                binding.rgSortOptions.setOnCheckedChangeListener { _, checkedId ->
                    val selectedSort = checkedId.toSortOption()
                    if (selectedSort != currentSort) {
                        onSortSelected(selectedSort)
                        onDismiss()
                    }
                }
            }
        }
    }
}

private fun BottomSheetActivitySortBinding.applyTexts(getLanguage: (String) -> String) {
    tvTitle.text = getLanguage(LanguageConst.ADD_PLAN_SORT_BY)
    rbPopularity.text = getLanguage(SortOption.POPULARITY.languageKey)
    rbRating.text = getLanguage(SortOption.RATING.languageKey)
    rbPriceLowToHigh.text = getLanguage(SortOption.PRICE_LOW_TO_HIGH.languageKey)
    rbDurationShortToLong.text = getLanguage(SortOption.DURATION_SHORT_TO_LONG.languageKey)
    rbDurationLongToShort.text = getLanguage(SortOption.DURATION_LONG_TO_SHORT.languageKey)
}

private fun BottomSheetActivitySortBinding.checkCurrent(currentSort: SortOption) {
    when (currentSort) {
        SortOption.POPULARITY -> rbPopularity.isChecked = true
        SortOption.RATING -> rbRating.isChecked = true
        SortOption.PRICE_LOW_TO_HIGH -> rbPriceLowToHigh.isChecked = true
        SortOption.DURATION_SHORT_TO_LONG -> rbDurationShortToLong.isChecked = true
        SortOption.DURATION_LONG_TO_SHORT -> rbDurationLongToShort.isChecked = true
        else -> Unit
    }
}

private fun Int.toSortOption(): SortOption = when (this) {
    R.id.rbPopularity -> SortOption.POPULARITY
    R.id.rbRating -> SortOption.RATING
    R.id.rbPriceLowToHigh -> SortOption.PRICE_LOW_TO_HIGH
    R.id.rbDurationShortToLong -> SortOption.DURATION_SHORT_TO_LONG
    R.id.rbDurationLongToShort -> SortOption.DURATION_LONG_TO_SHORT
    else -> SortOption.DEFAULT
}

/**
 * Compose counterpart of ActivityFilterBottomSheet: price and duration range
 * sliders with facet-driven bounds, Clear Selection and Confirm.
 *
 * @param minPriceBound / [maxPriceBound] / [minDurationBound] / [maxDurationBound]
 *   facet bounds; null falls back to the ActivityFilterData defaults
 */
@Composable
internal fun ActivityFilterSheet(
    currentFilter: ActivityFilterData,
    currency: String,
    minPriceBound: Float?,
    maxPriceBound: Float?,
    minDurationBound: Float?,
    maxDurationBound: Float?,
    getLanguage: (String) -> String,
    onFilterConfirmed: (ActivityFilterData) -> Unit,
    onDismiss: () -> Unit
) {
    TimelineSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            BindingHost(
                inflate = BottomSheetActivityFilterBinding::inflate,
                modifier = Modifier.fillMaxWidth()
            ) { binding, _, _ ->
                binding.root.hideLayoutDragHandle()
                val editor = ActivityFilterEditor(binding, currentFilter, currency, getLanguage)
                binding.tvTitle.text = getLanguage(LanguageConst.ADD_PLAN_FILTERS)
                binding.tvPriceLabel.text = getLanguage(LanguageConst.ADD_PLAN_FILTER_PRICE)
                binding.tvDurationLabel.text = getLanguage(LanguageConst.ADD_PLAN_FILTER_DURATION)
                binding.tvClearSelection.text = getLanguage(LanguageConst.ADD_PLAN_CLEAR_SELECTION)
                binding.btnConfirm.text = getLanguage(LanguageConst.CONFIRM)
                editor.setupSliders(minPriceBound, maxPriceBound, minDurationBound, maxDurationBound)
                binding.ivClose.setOnClickListener { onDismiss() }
                binding.tvClearSelection.setOnClickListener { editor.reset() }
                binding.btnConfirm.setOnClickListener {
                    onFilterConfirmed(editor.filter)
                    onDismiss()
                }
            }
        }
    }
}

/**
 * Holds the filter being edited and keeps the slider labels in sync. Facet
 * bounds are snapped to the step grid because the Material slider rejects a
 * range that is not a multiple of its step.
 */
private class ActivityFilterEditor(
    private val binding: BottomSheetActivityFilterBinding,
    var filter: ActivityFilterData,
    private val currency: String,
    private val getLanguage: (String) -> String
) {

    fun setupSliders(
        minPriceBound: Float?,
        maxPriceBound: Float?,
        minDurationBound: Float?,
        maxDurationBound: Float?
    ) {
        val context = binding.root.context
        val thumbDrawable = ContextCompat.getDrawable(context, R.drawable.trp_bg_slider_thumb)
        val thumbRadiusPx = (SLIDER_THUMB_RADIUS_DP * context.resources.displayMetrics.density).toInt()

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
                filter.minPrice.coerceIn(priceFrom, priceTo),
                filter.maxPrice.coerceIn(priceFrom, priceTo)
            )
            thumbRadius = thumbRadiusPx
            thumbDrawable?.let { setCustomThumbDrawable(it) }
            addOnChangeListener { slider, _, _ ->
                filter = filter.copy(minPrice = slider.values[0], maxPrice = slider.values[1])
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
                filter.minDuration.coerceIn(durationFrom, durationTo),
                filter.maxDuration.coerceIn(durationFrom, durationTo)
            )
            thumbRadius = thumbRadiusPx
            thumbDrawable?.let { setCustomThumbDrawable(it) }
            addOnChangeListener { slider, _, _ ->
                filter = filter.copy(minDuration = slider.values[0], maxDuration = slider.values[1])
                updateDurationLabels()
            }
        }

        updatePriceLabels()
        updateDurationLabels()
    }

    fun reset() {
        filter = ActivityFilterData.default()
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

    private fun updatePriceLabels() {
        binding.tvPriceMin.text = if (filter.minPrice == 0f) {
            getLanguage(LanguageConst.ADD_PLAN_FILTER_FREE)
        } else {
            formatPrice(filter.minPrice)
        }
        binding.tvPriceMax.text = formatPrice(filter.maxPrice)
    }

    private fun updateDurationLabels() {
        binding.tvDurationMin.text = FormatUtils.formatDuration(filter.minDuration)
        binding.tvDurationMax.text = FormatUtils.formatDuration(filter.maxDuration)
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
        return if (currency.uppercase() == "EUR") "$priceInt$symbol" else "$symbol$priceInt"
    }

    private fun snapDown(value: Float, step: Float): Float =
        if (step <= 0f) value else floor(value / step) * step

    private fun snapUp(value: Float, step: Float): Float =
        if (step <= 0f) value else ceil(value / step) * step
}

/** The sheet chrome already draws a drag handle; the layout's own handle is its first child. */
private fun View.hideLayoutDragHandle() {
    (this as? ViewGroup)?.getChildAt(0)?.visibility = View.GONE
}
