package com.tripian.trpcore.ui.timeline.addplan

import android.os.Bundle
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import com.tripian.one.api.cities.model.City
import com.tripian.trpcore.domain.model.timeline.AddPlanMode
import com.tripian.trpcore.repository.TripRepository
import io.mockk.every
import io.mockk.mockk
import java.util.ArrayList
import java.util.Calendar
import java.util.Date
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pins [AddPlanContainerVM.Companion.resolveInitialDayIndex] and the initial-selection
 * and day-selection behaviour it feeds into [AddPlanContainerVM.initializeFromArgs] and
 * [AddPlanContainerVM.selectDay] for a trip that includes past days.
 */
class AddPlanContainerVMTest {

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
    private val dayAfterTomorrow = dayOffsetFromToday(2)

    @Before
    fun setUp() {
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) = runnable.run()
            override fun postToMainThread(runnable: Runnable) = runnable.run()
            override fun isMainThread(): Boolean = true
        })
    }

    @After
    fun tearDown() {
        ArchTaskExecutor.getInstance().setDelegate(null)
    }

    @Test
    fun `test resolveInitialDayIndex returns NO_DAY_SELECTED for empty list`() {
        val result = AddPlanContainerVM.resolveInitialDayIndex(emptyList(), 0)
        assertEquals(AddPlanContainerVM.NO_DAY_SELECTED, result)
    }

    @Test
    fun `test resolveInitialDayIndex returns NO_DAY_SELECTED when all days are past`() {
        val days = listOf(dayOffsetFromToday(-3), dayOffsetFromToday(-2), yesterday)
        val result = AddPlanContainerVM.resolveInitialDayIndex(days, 0)
        assertEquals(AddPlanContainerVM.NO_DAY_SELECTED, result)
    }

    @Test
    fun `test resolveInitialDayIndex keeps requested index when it points at a future day`() {
        val days = listOf(yesterday, today, tomorrow, dayAfterTomorrow)
        val result = AddPlanContainerVM.resolveInitialDayIndex(days, 3)
        assertEquals(3, result)
    }

    @Test
    fun `test resolveInitialDayIndex keeps requested index when it points at today`() {
        val days = listOf(yesterday, today, tomorrow)
        val result = AddPlanContainerVM.resolveInitialDayIndex(days, 1)
        assertEquals(1, result)
    }

    @Test
    fun `test resolveInitialDayIndex falls back to today when requested index is past and today is present`() {
        val days = listOf(yesterday, today, tomorrow)
        val result = AddPlanContainerVM.resolveInitialDayIndex(days, 0)
        assertEquals(1, result)
    }

    @Test
    fun `test resolveInitialDayIndex falls back to first future day when requested index is past and today is absent`() {
        val days = listOf(dayOffsetFromToday(-2), yesterday, tomorrow, dayAfterTomorrow)
        val result = AddPlanContainerVM.resolveInitialDayIndex(days, 0)
        assertEquals(2, result)
    }

    @Test
    fun `test resolveInitialDayIndex remaps out of range requested index`() {
        val days = listOf(yesterday, today, tomorrow)
        val result = AddPlanContainerVM.resolveInitialDayIndex(days, 99)
        assertEquals(1, result)
    }

    private fun bundleOf(
        availableDays: ArrayList<Date>,
        cities: ArrayList<City>,
        selectedDayIndex: Int = 0
    ): Bundle {
        val bundle: Bundle = mockk(relaxed = true)
        every { bundle.getSerializable(AddPlanContainerVM.ARG_AVAILABLE_DAYS) } returns availableDays
        every { bundle.getSerializable(AddPlanContainerVM.ARG_CITIES) } returns cities
        every { bundle.getInt(AddPlanContainerVM.ARG_SELECTED_DAY_INDEX, 0) } returns selectedDayIndex
        every { bundle.getSerializable(AddPlanContainerVM.ARG_SELECTED_CITY) } returns null
        every { bundle.getString(AddPlanContainerVM.ARG_TRIP_HASH) } returns null
        every { bundle.getSerializable(AddPlanContainerVM.ARG_ACCOMMODATION) } returns null
        every { bundle.getSerializable(AddPlanContainerVM.ARG_BOOKED_ACTIVITIES) } returns null
        every { bundle.getSerializable(AddPlanContainerVM.ARG_PLANNED_ACTIVITY_IDS) } returns null
        every { bundle.getStringArrayList(AddPlanContainerVM.ARG_TRIP_WIDE_EXCLUDED_IDS) } returns null
        return bundle
    }

    private fun viewModel(): AddPlanContainerVM {
        val tripRepository: TripRepository = mockk(relaxed = true)
        return AddPlanContainerVM(tripRepository)
    }

    @Test
    fun `test initializeFromArgs with all past days selects nothing`() {
        val vm = viewModel()
        val days = arrayListOf(dayOffsetFromToday(-2), yesterday)
        val cities = arrayListOf(mockk<City>(relaxed = true))

        vm.initializeFromArgs(bundleOf(days, cities, selectedDayIndex = 1))

        assertEquals(AddPlanContainerVM.NO_DAY_SELECTED, vm.selectedDayIndex.value)
        assertNull(vm.planData.selectedDay)
        assertFalse(vm.continueButtonEnabled.value ?: true)
    }

    @Test
    fun `test initializeFromArgs remaps past requested index to first future day`() {
        val vm = viewModel()
        val days = arrayListOf(yesterday, today, tomorrow)
        val cities = arrayListOf(mockk<City>(relaxed = true))

        vm.initializeFromArgs(bundleOf(days, cities, selectedDayIndex = 0))

        assertEquals(1, vm.selectedDayIndex.value)
        assertEquals(today, vm.planData.selectedDay)
    }

    @Test
    fun `test selectDay ignores a past day index`() {
        val vm = viewModel()
        val days = arrayListOf(yesterday, today, tomorrow)
        val cities = arrayListOf(mockk<City>(relaxed = true))
        vm.initializeFromArgs(bundleOf(days, cities, selectedDayIndex = 1))

        vm.selectDay(0)

        assertEquals(1, vm.selectedDayIndex.value)
        assertEquals(today, vm.planData.selectedDay)
    }

    @Test
    fun `test selectDay ignores an out of range index`() {
        val vm = viewModel()
        val days = arrayListOf(yesterday, today, tomorrow)
        val cities = arrayListOf(mockk<City>(relaxed = true))
        vm.initializeFromArgs(bundleOf(days, cities, selectedDayIndex = 1))

        vm.selectDay(99)

        assertEquals(1, vm.selectedDayIndex.value)
        assertEquals(today, vm.planData.selectedDay)
    }

    @Test
    fun `test selectDay with future index and smart mode enables continue button`() {
        val vm = viewModel()
        val days = arrayListOf(yesterday, today, tomorrow)
        val city = mockk<City>(relaxed = true)
        val cities = arrayListOf(city)
        vm.initializeFromArgs(bundleOf(days, cities, selectedDayIndex = 1))

        vm.selectDay(2)
        vm.selectMode(AddPlanMode.SMART)

        assertEquals(2, vm.selectedDayIndex.value)
        assertEquals(tomorrow, vm.planData.selectedDay)
        assertTrue(vm.continueButtonEnabled.value ?: false)
    }
}
