package com.tripian.trpcore.ui.timeline.activity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** Pins [ActivityFilterData.withBoundsAsDefaults], the filter sheet's confirm mapping. */
class ActivityFilterDataTest {

    private fun ActivityFilterData.atBounds(): ActivityFilterData = withBoundsAsDefaults(
        priceFrom = 0f,
        priceTo = 380f,
        durationFrom = 30f,
        durationTo = 600f
    )

    @Test
    fun `test cleared sliders at their bounds map to default filter success`() {
        val cleared = ActivityFilterData(minPrice = 0f, maxPrice = 380f, minDuration = 30f, maxDuration = 600f)

        val confirmed = cleared.atBounds()

        assertEquals(ActivityFilterData.default(), confirmed)
        assertFalse(confirmed.hasActiveFilters())
        assertEquals(0, confirmed.activeFilterCount())
    }

    @Test
    fun `test default filter outside the slider bounds stays default success`() {
        assertEquals(ActivityFilterData.default(), ActivityFilterData.default().atBounds())
    }

    @Test
    fun `test values beyond the slider bounds map to default filter success`() {
        val beyond = ActivityFilterData(minPrice = 0f, maxPrice = 500f, minDuration = 0f, maxDuration = 900f)

        assertEquals(ActivityFilterData.default(), beyond.atBounds())
    }

    @Test
    fun `test inner values are kept success`() {
        val inner = ActivityFilterData(minPrice = 50f, maxPrice = 200f, minDuration = 60f, maxDuration = 180f)

        assertEquals(inner, inner.atBounds())
    }

    @Test
    fun `test only the thumbs at their bounds map to default success`() {
        val mixed = ActivityFilterData(minPrice = 0f, maxPrice = 200f, minDuration = 90f, maxDuration = 600f)

        val confirmed = mixed.atBounds()

        assertEquals(
            ActivityFilterData(
                minPrice = ActivityFilterData.DEFAULT_MIN_PRICE,
                maxPrice = 200f,
                minDuration = 90f,
                maxDuration = ActivityFilterData.DEFAULT_MAX_DURATION
            ),
            confirmed
        )
        assertEquals(2, confirmed.activeFilterCount())
    }
}
