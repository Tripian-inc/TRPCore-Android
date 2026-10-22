package com.tripian.trpcore.domain.usecase.timeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SegmentTimeRangeAdjustmentTest {

    @Test
    fun `test widenedRange returns null when step inside segment range success`() {
        val result = SegmentTimeRangeAdjustment.widenedRange(
            segmentStart = "2024-05-01 09:00",
            segmentEnd = "2024-05-01 18:00",
            stepStart = "2024-05-01 10:00",
            stepEnd = "2024-05-01 11:00"
        )

        assertNull(result)
    }

    @Test
    fun `test widenedRange widens start when step starts before segment success`() {
        val result = SegmentTimeRangeAdjustment.widenedRange(
            segmentStart = "2024-05-01 09:00",
            segmentEnd = "2024-05-01 18:00",
            stepStart = "2024-05-01 07:30",
            stepEnd = "2024-05-01 11:00"
        )

        assertEquals(SegmentTimeRange("2024-05-01 07:30", "2024-05-01 18:00"), result)
    }

    @Test
    fun `test widenedRange widens end when step ends after segment success`() {
        val result = SegmentTimeRangeAdjustment.widenedRange(
            segmentStart = "2024-05-01 09:00",
            segmentEnd = "2024-05-01 18:00",
            stepStart = "2024-05-01 10:00",
            stepEnd = "2024-05-01 20:15"
        )

        assertEquals(SegmentTimeRange("2024-05-01 09:00", "2024-05-01 20:15"), result)
    }

    @Test
    fun `test widenedRange widens both bounds when step spans wider than segment success`() {
        val result = SegmentTimeRangeAdjustment.widenedRange(
            segmentStart = "2024-05-01 09:00",
            segmentEnd = "2024-05-01 18:00",
            stepStart = "2024-05-01 07:00",
            stepEnd = "2024-05-01 20:00"
        )

        assertEquals(SegmentTimeRange("2024-05-01 07:00", "2024-05-01 20:00"), result)
    }

    @Test
    fun `test widenedRange widens end across a date boundary success`() {
        val result = SegmentTimeRangeAdjustment.widenedRange(
            segmentStart = "2024-05-01 09:00",
            segmentEnd = "2024-05-01 18:00",
            stepStart = "2024-05-02 00:30",
            stepEnd = "2024-05-02 01:30"
        )

        assertEquals(SegmentTimeRange("2024-05-01 09:00", "2024-05-02 01:30"), result)
    }

    @Test
    fun `test widenedRange treats null segmentEnd as segmentStart success`() {
        val result = SegmentTimeRangeAdjustment.widenedRange(
            segmentStart = "2024-05-01 09:00",
            segmentEnd = null,
            stepStart = "2024-05-01 09:00",
            stepEnd = "2024-05-01 11:00"
        )

        assertEquals(SegmentTimeRange("2024-05-01 09:00", "2024-05-01 11:00"), result)
    }

    @Test
    fun `test widenedRange keeps seconds format when widened segment bound had seconds and step does not success`() {
        val result = SegmentTimeRangeAdjustment.widenedRange(
            segmentStart = "2024-05-01 09:00:00",
            segmentEnd = "2024-05-01 18:00:00",
            stepStart = "2024-05-01 07:30",
            stepEnd = "2024-05-01 20:15"
        )

        assertEquals(SegmentTimeRange("2024-05-01 07:30:00", "2024-05-01 20:15:00"), result)
    }

    @Test
    fun `test widenedRange keeps no-seconds format when widened segment bound had no seconds and step has seconds success`() {
        val result = SegmentTimeRangeAdjustment.widenedRange(
            segmentStart = "2024-05-01 09:00",
            segmentEnd = "2024-05-01 18:00",
            stepStart = "2024-05-01 07:30:45",
            stepEnd = "2024-05-01 20:15:30"
        )

        assertEquals(SegmentTimeRange("2024-05-01 07:30", "2024-05-01 20:15"), result)
    }

    @Test
    fun `test widenedRange returns null when segmentStart is null success`() {
        val result = SegmentTimeRangeAdjustment.widenedRange(
            segmentStart = null,
            segmentEnd = "2024-05-01 18:00",
            stepStart = "2024-05-01 07:00",
            stepEnd = "2024-05-01 20:00"
        )

        assertNull(result)
    }

    @Test
    fun `test widenedRange returns null when a bound is unparseable success`() {
        val result = SegmentTimeRangeAdjustment.widenedRange(
            segmentStart = "2024-05-01 09:00",
            segmentEnd = "2024-05-01 18:00",
            stepStart = "not-a-date",
            stepEnd = "2024-05-01 20:00"
        )

        assertNull(result)
    }

    @Test
    fun `test stepWindow collapses to start when endTime is null success`() {
        val result = SegmentTimeRangeAdjustment.stepWindow("2024-05-01", "09:00", null)

        assertEquals(SegmentTimeRange("2024-05-01 09:00", "2024-05-01 09:00"), result)
    }

    @Test
    fun `test stepWindow keeps end on same day when end is after start success`() {
        val result = SegmentTimeRangeAdjustment.stepWindow("2024-05-01", "09:00", "11:00")

        assertEquals(SegmentTimeRange("2024-05-01 09:00", "2024-05-01 11:00"), result)
    }

    @Test
    fun `test stepWindow pushes end to next day when end is before start success`() {
        val result = SegmentTimeRangeAdjustment.stepWindow("2024-05-01", "23:00", "01:00")

        assertEquals(SegmentTimeRange("2024-05-01 23:00", "2024-05-02 01:00"), result)
    }

    @Test
    fun `test stepWindow rolls over month when next day crosses month boundary success`() {
        val result = SegmentTimeRangeAdjustment.stepWindow("2024-04-30", "23:30", "00:30")

        assertEquals(SegmentTimeRange("2024-04-30 23:30", "2024-05-01 00:30"), result)
    }

    @Test
    fun `test stepWindow rolls over year when next day crosses year boundary success`() {
        val result = SegmentTimeRangeAdjustment.stepWindow("2024-12-31", "23:30", "00:30")

        assertEquals(SegmentTimeRange("2024-12-31 23:30", "2025-01-01 00:30"), result)
    }

    @Test
    fun `test stepWindow returns null for unparseable date success`() {
        val result = SegmentTimeRangeAdjustment.stepWindow("not-a-date", "09:00", "11:00")

        assertNull(result)
    }

    @Test
    fun `test stepWindow returns null for unparseable time success`() {
        val result = SegmentTimeRangeAdjustment.stepWindow("2024-05-01", "9am", "11:00")

        assertNull(result)
    }

    @Test
    fun `test datePartOf extracts date from datetime with space separator success`() {
        assertEquals("2024-05-01", SegmentTimeRangeAdjustment.datePartOf("2024-05-01 09:00"))
    }

    @Test
    fun `test datePartOf extracts date from datetime with T separator success`() {
        assertEquals("2024-05-01", SegmentTimeRangeAdjustment.datePartOf("2024-05-01T09:00:00"))
    }

    @Test
    fun `test datePartOf returns null for blank or null input success`() {
        assertNull(SegmentTimeRangeAdjustment.datePartOf(null))
        assertNull(SegmentTimeRangeAdjustment.datePartOf(""))
    }

    @Test
    fun `test timePartOf extracts time from datetime with space separator success`() {
        assertEquals("09:00", SegmentTimeRangeAdjustment.timePartOf("2024-05-01 09:00"))
    }

    @Test
    fun `test timePartOf extracts time with seconds from datetime with T separator success`() {
        assertEquals("09:00:00", SegmentTimeRangeAdjustment.timePartOf("2024-05-01T09:00:00"))
    }

    @Test
    fun `test timePartOf returns null when there is no time part success`() {
        assertNull(SegmentTimeRangeAdjustment.timePartOf("2024-05-01"))
        assertNull(SegmentTimeRangeAdjustment.timePartOf(null))
    }
}
