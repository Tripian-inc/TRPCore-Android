package com.tripian.trpcore.domain.model.timeline

import com.tripian.one.api.cities.model.City
import com.tripian.one.api.pois.model.Coordinate
import io.mockk.mockk
import java.util.Calendar
import java.util.Date
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins [AddPlanData.hasSelectableDay], [AddPlanData.canContinueFromSelectDay] and
 * [AddPlanData.isValidForSmartMode]/[AddPlanData.isValidForManualMode] against a past,
 * today and future [selectedDay].
 */
class AddPlanDataTest {

    private val now: Date = Date()

    private fun dayOffsetFromToday(offsetDays: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = now
        calendar.add(Calendar.DAY_OF_YEAR, offsetDays)
        return calendar.time
    }

    private val yesterday = dayOffsetFromToday(-1)
    private val today = dayOffsetFromToday(0)
    private val tomorrow = dayOffsetFromToday(1)

    private fun planData(
        selectedDay: Date?,
        selectedCity: City? = mockk(relaxed = true),
        selectedMode: AddPlanMode = AddPlanMode.SMART,
        selectedManualCategory: ManualCategory? = null
    ): AddPlanData {
        return AddPlanData(
            selectedDay = selectedDay,
            selectedCity = selectedCity,
            selectedMode = selectedMode,
            selectedManualCategory = selectedManualCategory
        )
    }

    @Test
    fun `test hasSelectableDay false when day null`() {
        assertFalse(planData(selectedDay = null).hasSelectableDay())
    }

    @Test
    fun `test hasSelectableDay false when day is past`() {
        assertFalse(planData(selectedDay = yesterday).hasSelectableDay())
    }

    @Test
    fun `test hasSelectableDay true when day is today`() {
        assertTrue(planData(selectedDay = today).hasSelectableDay())
    }

    @Test
    fun `test hasSelectableDay true when day is future`() {
        assertTrue(planData(selectedDay = tomorrow).hasSelectableDay())
    }

    @Test
    fun `test canContinueFromSelectDay false when selectedDay null`() {
        assertFalse(planData(selectedDay = null).canContinueFromSelectDay())
    }

    @Test
    fun `test canContinueFromSelectDay false when selectedDay past`() {
        assertFalse(planData(selectedDay = yesterday).canContinueFromSelectDay())
    }

    @Test
    fun `test canContinueFromSelectDay true for today with smart mode`() {
        assertTrue(planData(selectedDay = today).canContinueFromSelectDay())
    }

    @Test
    fun `test canContinueFromSelectDay true for future day with smart mode`() {
        assertTrue(planData(selectedDay = tomorrow).canContinueFromSelectDay())
    }

    @Test
    fun `test canContinueFromSelectDay false when city missing`() {
        assertFalse(planData(selectedDay = today, selectedCity = null).canContinueFromSelectDay())
    }

    @Test
    fun `test canContinueFromSelectDay false for manual mode without category`() {
        val data = planData(
            selectedDay = today,
            selectedMode = AddPlanMode.MANUAL,
            selectedManualCategory = null
        )
        assertFalse(data.canContinueFromSelectDay())
    }

    @Test
    fun `test canContinueFromSelectDay true for manual mode with category`() {
        val data = planData(
            selectedDay = today,
            selectedMode = AddPlanMode.MANUAL,
            selectedManualCategory = ManualCategory.ACTIVITIES
        )
        assertTrue(data.canContinueFromSelectDay())
    }

    @Test
    fun `test canContinueFromSelectDay false for none mode`() {
        val data = planData(selectedDay = today, selectedMode = AddPlanMode.NONE)
        assertFalse(data.canContinueFromSelectDay())
    }

    private fun validSmartData(selectedDay: Date?): AddPlanData {
        return planData(selectedDay = selectedDay, selectedMode = AddPlanMode.SMART).apply {
            startTime = "10:00"
            endTime = "18:00"
            startingPointLocation = mockk<Coordinate>(relaxed = true)
            selectedSmartCategories = mutableListOf(SmartCategory.POI)
        }
    }

    @Test
    fun `test isValidForSmartMode false when day is past`() {
        assertFalse(validSmartData(selectedDay = yesterday).isValidForSmartMode())
    }

    @Test
    fun `test isValidForSmartMode true when day is today and otherwise valid`() {
        assertTrue(validSmartData(selectedDay = today).isValidForSmartMode())
    }

    @Test
    fun `test isValidForSmartMode true when day is future and otherwise valid`() {
        assertTrue(validSmartData(selectedDay = tomorrow).isValidForSmartMode())
    }

    @Test
    fun `test isValidForManualMode false when day is past`() {
        val data = planData(
            selectedDay = yesterday,
            selectedMode = AddPlanMode.MANUAL,
            selectedManualCategory = ManualCategory.ACTIVITIES
        )
        assertFalse(data.isValidForManualMode())
    }

    @Test
    fun `test isValidForManualMode true when day is today`() {
        val data = planData(
            selectedDay = today,
            selectedMode = AddPlanMode.MANUAL,
            selectedManualCategory = ManualCategory.ACTIVITIES
        )
        assertTrue(data.isValidForManualMode())
    }
}
