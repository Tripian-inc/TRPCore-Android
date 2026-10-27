package com.tripian.trpcore.ui.timeline

import android.os.Looper
import androidx.lifecycle.MutableLiveData
import com.tripian.one.api.timeline.model.SegmentType
import com.tripian.one.api.timeline.model.Timeline
import com.tripian.one.api.timeline.model.TimelinePlan
import com.tripian.one.api.timeline.model.TimelineProfile
import com.tripian.one.api.timeline.model.TimelineSegment
import com.tripian.one.api.timeline.model.TimelineStep
import com.tripian.trpcore.domain.DoLightLogin
import com.tripian.trpcore.domain.manager.AvailabilityCheckManager
import com.tripian.trpcore.domain.usecase.timeline.CreateSegmentUseCase
import com.tripian.trpcore.domain.usecase.timeline.CreateTimelineUseCase
import com.tripian.trpcore.domain.usecase.timeline.DeleteSegmentUseCase
import com.tripian.trpcore.domain.usecase.timeline.DeleteStepUseCase
import com.tripian.trpcore.domain.usecase.timeline.FetchTimelineUseCase
import com.tripian.trpcore.domain.usecase.timeline.GetTimelineStepRoutesUseCase
import com.tripian.trpcore.domain.usecase.timeline.ResolveCitiesUseCase
import com.tripian.trpcore.domain.usecase.timeline.UpdateSegmentTimeUseCase
import com.tripian.trpcore.domain.usecase.timeline.UpdateStepTimeUseCase
import com.tripian.trpcore.domain.usecase.timeline.WaitForGenerationUseCase
import com.tripian.trpcore.domain.usecase.timeline.sync.AddMissingBookedActivitiesUseCase
import com.tripian.trpcore.domain.usecase.timeline.sync.DetectReservedToBookedTransitionUseCase
import com.tripian.trpcore.domain.usecase.timeline.sync.RemoveObsoleteSegmentsUseCase
import com.tripian.trpcore.domain.usecase.timeline.sync.ResolveCityIdsForActivitiesUseCase
import com.tripian.trpcore.domain.usecase.timeline.sync.SyncReservedToBookedUseCase
import com.tripian.trpcore.domain.usecase.timeline.sync.UpdateDateRangeUseCase
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.repository.TripRepository
import com.tripian.trpcore.repository.MiscRepository
import com.tripian.trpcore.ui.timeline.mapper.MapItemMapper
import com.tripian.trpcore.ui.timeline.mapper.TimelineDisplayItemBuilder
import com.tripian.trpcore.util.Preferences
import com.tripian.trpcore.util.Strings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Pins [ACTimelineVM.updateStepTime]'s segment-widening contract: a step that already
 * fits its segment's window moves on its own, a step outside the window widens the
 * segment first (segment update before step update), and a failed widening attempt
 * must stop the step update from ever being sent.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ACTimelineVMUpdateStepTimeTest {

    private val updateStepTimeUseCase: UpdateStepTimeUseCase = mockk(relaxed = true)
    private val updateSegmentTimeUseCase: UpdateSegmentTimeUseCase = mockk(relaxed = true)
    private val fetchTimelineUseCase: FetchTimelineUseCase = mockk(relaxed = true)
    private val mainLooper: Looper = mockk(relaxed = true)

    private val stepId = 42

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockkStatic(Looper::class)
        every { Looper.getMainLooper() } returns mainLooper
        every { Looper.myLooper() } returns mainLooper
        every { mainLooper.thread } returns Thread.currentThread()
    }

    @After
    fun tearDown() {
        unmockkStatic(Looper::class)
        Dispatchers.resetMain()
    }

    private fun segment(type: String = SegmentType.ITINERARY) = TimelineSegment().apply {
        segmentType = type
        startDate = "2024-05-01 09:00"
        endDate = "2024-05-01 18:00"
    }

    private fun timelineWithStep(segment: TimelineSegment, stepStart: String, stepEnd: String): Timeline {
        val step = TimelineStep().apply {
            id = stepId
            startDateTimes = stepStart
            endDateTimes = stepEnd
        }
        val plan = TimelinePlan().apply {
            startDate = "2024-05-01"
            steps = listOf(step)
        }
        return Timeline().apply {
            tripHash = "trip-1"
            plans = listOf(plan)
            tripProfile = TimelineProfile().apply { segments = listOf(segment) }
        }
    }

    private fun vm(): ACTimelineVM {
        val vm = ACTimelineVM(
            doLightLogin = mockk<DoLightLogin>(relaxed = true),
            fetchTimelineUseCase = fetchTimelineUseCase,
            createTimelineUseCase = mockk<CreateTimelineUseCase>(relaxed = true),
            createSegmentUseCase = mockk<CreateSegmentUseCase>(relaxed = true),
            waitForGenerationUseCase = mockk<WaitForGenerationUseCase>(relaxed = true),
            deleteSegmentUseCase = mockk<DeleteSegmentUseCase>(relaxed = true),
            deleteStepUseCase = mockk<DeleteStepUseCase>(relaxed = true),
            updateStepTimeUseCase = updateStepTimeUseCase,
            updateSegmentTimeUseCase = updateSegmentTimeUseCase,
            getTimelineStepRoutesUseCase = mockk<GetTimelineStepRoutesUseCase>(relaxed = true),
            resolveCitiesUseCase = mockk<ResolveCitiesUseCase>(relaxed = true),
            tripRepository = mockk<TripRepository>(relaxed = true),
            timelineRepository = mockk<TimelineRepository>(relaxed = true),
            preferences = mockk<Preferences>(relaxed = true),
            resolveCityIdsForActivitiesUseCase = mockk<ResolveCityIdsForActivitiesUseCase>(relaxed = true),
            detectReservedToBookedTransitionUseCase = mockk<DetectReservedToBookedTransitionUseCase>(relaxed = true),
            syncReservedToBookedUseCase = mockk<SyncReservedToBookedUseCase>(relaxed = true),
            addMissingBookedActivitiesUseCase = mockk<AddMissingBookedActivitiesUseCase>(relaxed = true),
            updateDateRangeUseCase = mockk<UpdateDateRangeUseCase>(relaxed = true),
            removeObsoleteSegmentsUseCase = mockk<RemoveObsoleteSegmentsUseCase>(relaxed = true),
            availabilityCheckManager = mockk<AvailabilityCheckManager>(relaxed = true),
            mapItemMapper = mockk<MapItemMapper>(relaxed = true),
            displayItemBuilder = mockk<TimelineDisplayItemBuilder>(relaxed = true)
        )
        vm.strings = mockk<Strings>(relaxed = true)
        vm.miscRepository = mockk<MiscRepository>(relaxed = true)
        return vm
    }

    private fun seedTimeline(vm: ACTimelineVM, timeline: Timeline, tripHash: String) {
        val timelineField = ACTimelineVM::class.java.getDeclaredField("_timeline")
        timelineField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (timelineField.get(vm) as MutableLiveData<Timeline>).value = timeline

        val tripHashField = ACTimelineVM::class.java.getDeclaredField("_tripHash")
        tripHashField.isAccessible = true
        tripHashField.set(vm, tripHash)
    }

    @Test
    fun `test updateStepTime only calls step use case when step stays inside segment range success`() = runTest {
        val vm = vm()
        val timeline = timelineWithStep(segment(), "2024-05-01 10:00", "2024-05-01 11:00")
        seedTimeline(vm, timeline, "trip-1")
        coEvery { fetchTimelineUseCase(any()) } returns timeline

        vm.updateStepTime(stepId, "10:30", "11:30")

        coVerify(exactly = 0) { updateSegmentTimeUseCase(any()) }
        coVerify(exactly = 1) { updateStepTimeUseCase(any()) }
    }

    @Test
    fun `test updateStepTime widens segment before updating step when step starts before segment success`() = runTest {
        val vm = vm()
        val timeline = timelineWithStep(segment(), "2024-05-01 10:00", "2024-05-01 11:00")
        seedTimeline(vm, timeline, "trip-1")
        coEvery { fetchTimelineUseCase(any()) } throws RuntimeException("offline")

        vm.updateStepTime(stepId, "07:00", "11:00")

        coVerifyOrder {
            updateSegmentTimeUseCase(any())
            updateStepTimeUseCase(any())
        }
    }

    @Test
    fun `test updateStepTime passes widened bounds and segment index to segment use case success`() = runTest {
        val vm = vm()
        val timeline = timelineWithStep(segment(), "2024-05-01 10:00", "2024-05-01 11:00")
        seedTimeline(vm, timeline, "trip-1")
        coEvery { fetchTimelineUseCase(any()) } throws RuntimeException("offline")

        vm.updateStepTime(stepId, "07:00", "11:00")

        coVerify {
            updateSegmentTimeUseCase(
                withArg { params ->
                    assertEquals(0, params.segmentIndex)
                    assertEquals("trip-1", params.tripHash)
                    assertEquals("07:00", params.newStartTime)
                }
            )
        }
    }

    @Test
    fun `test updateStepTime leaves a booked segment untouched when step moves outside its range success`() = runTest {
        val vm = vm()
        val timeline = timelineWithStep(
            segment(SegmentType.BOOKED_ACTIVITY), "2024-05-01 10:00", "2024-05-01 11:00"
        )
        seedTimeline(vm, timeline, "trip-1")
        coEvery { fetchTimelineUseCase(any()) } returns timeline

        vm.updateStepTime(stepId, "07:00", "11:00")

        coVerify(exactly = 0) { updateSegmentTimeUseCase(any()) }
        coVerify(exactly = 1) { updateStepTimeUseCase(any()) }
    }

    @Test
    fun `test updateStepTime leaves a reserved segment untouched when step moves outside its range success`() = runTest {
        val vm = vm()
        val timeline = timelineWithStep(
            segment(SegmentType.RESERVED_ACTIVITY), "2024-05-01 10:00", "2024-05-01 11:00"
        )
        seedTimeline(vm, timeline, "trip-1")
        coEvery { fetchTimelineUseCase(any()) } returns timeline

        vm.updateStepTime(stepId, "07:00", "11:00")

        coVerify(exactly = 0) { updateSegmentTimeUseCase(any()) }
        coVerify(exactly = 1) { updateStepTimeUseCase(any()) }
    }

    @Test
    fun `test updateStepTime never calls step use case when widening the segment fails success`() = runTest {
        val vm = vm()
        val timeline = timelineWithStep(segment(), "2024-05-01 10:00", "2024-05-01 11:00")
        seedTimeline(vm, timeline, "trip-1")
        coEvery { updateSegmentTimeUseCase(any()) } throws RuntimeException("segment update failed")

        vm.updateStepTime(stepId, "07:00", "11:00")

        coVerify(exactly = 0) { updateStepTimeUseCase(any()) }
        assertEquals("segment update failed", vm.error.value)
    }

    @Test
    fun `test updateStepTime refetches timeline and reports step error when step update fails after widening`() = runTest {
        val vm = vm()
        val timeline = timelineWithStep(segment(), "2024-05-01 10:00", "2024-05-01 11:00")
        seedTimeline(vm, timeline, "trip-1")
        coEvery { updateStepTimeUseCase(any()) } throws RuntimeException("step update failed")
        coEvery { fetchTimelineUseCase(any()) } throws RuntimeException("offline")

        vm.updateStepTime(stepId, "07:00", "11:00")

        coVerify(exactly = 1) { updateSegmentTimeUseCase(any()) }
        coVerify(exactly = 1) { fetchTimelineUseCase(any()) }
        assertEquals("step update failed", vm.error.value)
    }

    @Test
    fun `test updateStepTime skips refetch when step update fails without widening`() = runTest {
        val vm = vm()
        val timeline = timelineWithStep(segment(), "2024-05-01 10:00", "2024-05-01 11:00")
        seedTimeline(vm, timeline, "trip-1")
        coEvery { updateStepTimeUseCase(any()) } throws RuntimeException("step update failed")

        vm.updateStepTime(stepId, "10:30", "11:30")

        coVerify(exactly = 0) { fetchTimelineUseCase(any()) }
        assertEquals("step update failed", vm.error.value)
    }

    @Test
    fun `test updateStepTime applies widened segment range locally when refetch fails`() = runTest {
        val vm = vm()
        val segment = segment()
        val timeline = timelineWithStep(segment, "2024-05-01 10:00", "2024-05-01 11:00")
        seedTimeline(vm, timeline, "trip-1")
        coEvery { updateStepTimeUseCase(any()) } returns TimelineStep()
        coEvery { fetchTimelineUseCase(any()) } throws RuntimeException("fetch failed")

        vm.updateStepTime(stepId, "07:00", "08:00")

        assertEquals("2024-05-01 07:00", segment.startDate)
        assertEquals("2024-05-01 18:00", segment.endDate)
        val step = timeline.plans!!.first().steps!!.first()
        assertEquals("2024-05-01 07:00", step.startDateTimes)
        assertEquals("2024-05-01 08:00", step.endDateTimes)
    }

    @Test
    fun `test updateStepTime applies widened segment range locally when step update and refetch fail`() = runTest {
        val vm = vm()
        val segment = segment()
        val timeline = timelineWithStep(segment, "2024-05-01 10:00", "2024-05-01 11:00")
        seedTimeline(vm, timeline, "trip-1")
        coEvery { updateStepTimeUseCase(any()) } throws RuntimeException("step update failed")
        coEvery { fetchTimelineUseCase(any()) } throws RuntimeException("offline")

        vm.updateStepTime(stepId, "07:00", "08:00")

        assertEquals("2024-05-01 07:00", segment.startDate)
        assertEquals("2024-05-01 18:00", segment.endDate)
        val step = timeline.plans!!.first().steps!!.first()
        assertEquals("2024-05-01 10:00", step.startDateTimes)
        assertEquals("step update failed", vm.error.value)
    }
}
