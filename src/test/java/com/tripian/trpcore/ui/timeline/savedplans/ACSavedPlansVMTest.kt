package com.tripian.trpcore.ui.timeline.savedplans

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import com.tripian.trpcore.domain.usecase.timeline.CreateReservedActivityFromFavoriteUseCase
import com.tripian.trpcore.domain.usecase.timeline.WaitForGenerationUseCase
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.ui.timeline.addplan.AddPlanContainerVM
import com.tripian.trpcore.util.Preferences
import io.mockk.mockk
import java.util.Calendar
import java.util.Date
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Pins the day carried over from the timeline into [ACSavedPlansVM.initialize] and the
 * [AddPlanContainerVM.resolveInitialDayIndex] rule it relies on for a trip with past days.
 */
class ACSavedPlansVMTest {

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

    private fun viewModel(): ACSavedPlansVM {
        val createReservedActivityFromFavoriteUseCase: CreateReservedActivityFromFavoriteUseCase =
            mockk(relaxed = true)
        val waitForGenerationUseCase: WaitForGenerationUseCase = mockk(relaxed = true)
        val timelineRepository: TimelineRepository = mockk(relaxed = true)
        val preferences: Preferences = mockk(relaxed = true)
        return ACSavedPlansVM(
            createReservedActivityFromFavoriteUseCase,
            waitForGenerationUseCase,
            timelineRepository,
            preferences
        )
    }

    @Test
    fun `test initialize keeps selected index when it points at today success`() {
        val vm = viewModel()
        val days = listOf(yesterday, today, tomorrow)

        vm.initialize(favorites = emptyList(), tripHash = "", availableDays = days, selectedDayIndex = 1)

        assertEquals(today, vm.getSelectedDate())
    }

    @Test
    fun `test initialize keeps selected index when it points at a future day success`() {
        val vm = viewModel()
        val days = listOf(yesterday, today, tomorrow, dayAfterTomorrow)

        vm.initialize(favorites = emptyList(), tripHash = "", availableDays = days, selectedDayIndex = 3)

        assertEquals(dayAfterTomorrow, vm.getSelectedDate())
    }

    @Test
    fun `test initialize remaps past selected index to today when today is present success`() {
        val vm = viewModel()
        val days = listOf(yesterday, today, tomorrow)

        vm.initialize(favorites = emptyList(), tripHash = "", availableDays = days, selectedDayIndex = 0)

        assertEquals(today, vm.getSelectedDate())
    }

    @Test
    fun `test initialize remaps past selected index to first future day when today is absent success`() {
        val vm = viewModel()
        val days = listOf(dayOffsetFromToday(-2), yesterday, tomorrow, dayAfterTomorrow)

        vm.initialize(favorites = emptyList(), tripHash = "", availableDays = days, selectedDayIndex = 0)

        assertEquals(tomorrow, vm.getSelectedDate())
    }

    @Test
    fun `test initialize selects nothing when every day is past success`() {
        val vm = viewModel()
        val days = listOf(dayOffsetFromToday(-2), yesterday)

        vm.initialize(favorites = emptyList(), tripHash = "", availableDays = days, selectedDayIndex = 1)

        assertNull(vm.getSelectedDate())
    }

    @Test
    fun `test initialize remaps out of range selected index like a past day success`() {
        val vm = viewModel()
        val days = listOf(yesterday, today, tomorrow)

        vm.initialize(favorites = emptyList(), tripHash = "", availableDays = days, selectedDayIndex = 99)

        assertEquals(today, vm.getSelectedDate())
    }

    @Test
    fun `test initialize with empty available days selects nothing success`() {
        val vm = viewModel()

        vm.initialize(favorites = emptyList(), tripHash = "", availableDays = emptyList(), selectedDayIndex = 0)

        assertNull(vm.getSelectedDate())
    }
}
