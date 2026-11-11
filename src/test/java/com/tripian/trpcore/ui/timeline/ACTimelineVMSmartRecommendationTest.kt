package com.tripian.trpcore.ui.timeline

import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import androidx.lifecycle.MutableLiveData
import com.tripian.one.api.cities.model.City
import com.tripian.one.api.timeline.model.SegmentType
import com.tripian.one.api.timeline.model.Timeline
import com.tripian.one.api.timeline.model.TimelinePlan
import com.tripian.one.api.timeline.model.TimelineProfile
import com.tripian.one.api.timeline.model.TimelineSegment
import com.tripian.one.api.timeline.model.TimelineStep
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.domain.DoLightLogin
import com.tripian.trpcore.domain.manager.AvailabilityCheckManager
import com.tripian.trpcore.domain.model.timeline.AddPlanData
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
import com.tripian.trpcore.repository.MiscRepository
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.repository.TripRepository
import com.tripian.trpcore.repository.base.ResponseModelBase
import com.tripian.trpcore.ui.timeline.mapper.MapItemMapper
import com.tripian.trpcore.ui.timeline.mapper.TimelineDisplayItemBuilder
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.Preferences
import com.tripian.trpcore.util.Strings
import com.tripian.trpcore.util.ViewListener
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

/**
 * Pins [ACTimelineVM]'s smart-recommendation create/poll flow: the foreground loader phase,
 * the switch to a silent background poller after [ACTimelineVM] holds the loader open for
 * 20 attempts, success/failure conclusion (apply vs. best-effort delete) and the resume
 * of pending generations found on a freshly loaded timeline.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ACTimelineVMSmartRecommendationTest {

    private val testDispatcher = StandardTestDispatcher()

    private val createSegmentUseCase: CreateSegmentUseCase = mockk(relaxed = true)
    private val waitForGenerationUseCase: WaitForGenerationUseCase = mockk(relaxed = true)
    private val deleteSegmentUseCase: DeleteSegmentUseCase = mockk(relaxed = true)
    private val fetchTimelineUseCase: FetchTimelineUseCase = mockk(relaxed = true)
    private val timelineRepository: TimelineRepository = mockk(relaxed = true)
    private val tripRepository: TripRepository = mockk(relaxed = true)
    private val viewListener: ViewListener = mockk(relaxed = true)
    private val mainLooper: Looper = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Looper::class)
        every { Looper.getMainLooper() } returns mainLooper
        every { Looper.myLooper() } returns mainLooper
        every { mainLooper.thread } returns Thread.currentThread()
        mockkStatic(TextUtils::class)
        every { TextUtils.isEmpty(any()) } answers { (it.invocation.args[0] as? String).isNullOrEmpty() }
        coEvery { createSegmentUseCase(any()) } returns ResponseModelBase().apply { status = 200 }
        TRPCore.core = mockk(relaxed = true)
        mockkConstructor(Handler::class)
        every { anyConstructed<Handler>().post(any()) } answers {
            firstArg<Runnable>().run()
            true
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    private fun smartSegment(
        title: String = "Recommendations-token",
        smart: Boolean? = true
    ) = TimelineSegment().apply {
        segmentType = SegmentType.ITINERARY
        this.title = title
        smartRecommendation = smart
        startDate = "2024-05-01 09:00"
        endDate = "2024-05-01 18:00"
    }

    private fun plan(id: String, status: Int, hasSteps: Boolean = true) = TimelinePlan().apply {
        this.id = id
        generatedStatus = status
        steps = if (hasSteps) listOf(TimelineStep()) else emptyList()
        startDate = "2024-05-01"
        endDate = "2024-05-01"
    }

    private fun timelineOf(
        segments: List<TimelineSegment>,
        plans: List<TimelinePlan>,
        tripHash: String = "trip-1"
    ) = Timeline().apply {
        this.tripHash = tripHash
        this.plans = plans
        this.tripProfile = TimelineProfile().apply { this.segments = segments }
    }

    private fun vm(): ACTimelineVM {
        val vm = ACTimelineVM(
            doLightLogin = mockk<DoLightLogin>(relaxed = true),
            fetchTimelineUseCase = fetchTimelineUseCase,
            createTimelineUseCase = mockk<CreateTimelineUseCase>(relaxed = true),
            createSegmentUseCase = createSegmentUseCase,
            waitForGenerationUseCase = waitForGenerationUseCase,
            deleteSegmentUseCase = deleteSegmentUseCase,
            deleteStepUseCase = mockk<DeleteStepUseCase>(relaxed = true),
            updateStepTimeUseCase = mockk<UpdateStepTimeUseCase>(relaxed = true),
            updateSegmentTimeUseCase = mockk<UpdateSegmentTimeUseCase>(relaxed = true),
            getTimelineStepRoutesUseCase = mockk<GetTimelineStepRoutesUseCase>(relaxed = true),
            resolveCitiesUseCase = mockk<ResolveCitiesUseCase>(relaxed = true),
            tripRepository = tripRepository,
            timelineRepository = timelineRepository,
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
        vm.viewListener = viewListener
        return vm
    }

    private fun seedTimeline(vm: ACTimelineVM, timeline: Timeline, tripHash: String = "trip-1") {
        val timelineField = ACTimelineVM::class.java.getDeclaredField("_timeline")
        timelineField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (timelineField.get(vm) as MutableLiveData<Timeline>).value = timeline

        val tripHashField = ACTimelineVM::class.java.getDeclaredField("_tripHash")
        tripHashField.isAccessible = true
        tripHashField.set(vm, tripHash)
    }

    private fun addPlanData() = AddPlanData(
        selectedDay = Date(),
        selectedDayIndex = 0,
        selectedCity = mockk<City>(relaxed = true).also { every { it.id } returns 109 },
        startTime = "10:00",
        endTime = "18:00",
        travelers = 2
    )

    /** Wires [waitForGenerationUseCase] to the real polling loop driven by [fetch]. */
    private fun stubPolling(fetch: suspend (attempt: Int) -> Timeline) {
        coEvery { waitForGenerationUseCase(any()) } coAnswers {
            var attempt = 0
            val params = firstArg<WaitForGenerationUseCase.Params>()
            WaitForGenerationUseCase.poll(params) {
                attempt++
                fetch(attempt)
            }
        }
    }

    @Test
    fun `createSmartRecommendationSegment applies result and scrolls when generated within foreground attempts`() =
        runTest(testDispatcher) {
            val vm = vm()
            seedTimeline(vm, timelineOf(emptyList(), emptyList()))
            stubPolling { attempt ->
                if (attempt < 5) {
                    timelineOf(listOf(smartSegment()), listOf(plan("p1", status = 0)))
                } else {
                    timelineOf(listOf(smartSegment()), listOf(plan("p1", status = 1)))
                }
            }

            vm.createSmartRecommendationSegment(addPlanData())
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals("p1", vm.scrollToNewSegmentPlanId.value)
            assertEquals(1, vm.timeline.value?.plans?.first()?.generatedStatus)
            coVerify(exactly = 0) { fetchTimelineUseCase(any()) }
            coVerify(exactly = 0) { viewListener.showAlert(any(), any()) }
        }

    @Test
    fun `createSmartRecommendationSegment hides loader once and shows continues notice after 20 attempts then best-effort deletes on timeout`() =
        runTest(testDispatcher) {
            val vm = vm()
            seedTimeline(vm, timelineOf(emptyList(), emptyList()))
            var fetchCalls = 0
            stubPolling { fetchCalls++; timelineOf(listOf(smartSegment()), listOf(plan("p1", status = 0, hasSteps = false))) }
            coEvery { fetchTimelineUseCase(any()) } returns
                timelineOf(listOf(smartSegment()), listOf(plan("p1", status = 0, hasSteps = false)))

            vm.createSmartRecommendationSegment(addPlanData())
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(60, fetchCalls)
            coVerify(exactly = 1) {
                viewListener.showAlert(AlertType.INFO, "Your recommendations are still being prepared. The list will update automatically when they're ready.")
            }
            coVerify(exactly = 1) {
                viewListener.showAlert(AlertType.ERROR, "We couldn't generate recommendations right now. Please try again.")
            }
            coVerify(exactly = 1) { deleteSegmentUseCase(DeleteSegmentUseCase.Params("trip-1", 0)) }
            assertNull(vm.scrollToNewSegmentPlanId.value)
        }

    @Test
    fun `background success at attempt 25 silently refreshes and shows ready notice only when screen is visible`() =
        runTest(testDispatcher) {
            val vm = vm()
            seedTimeline(vm, timelineOf(emptyList(), emptyList()))
            stubPolling { attempt ->
                if (attempt < 25) {
                    timelineOf(listOf(smartSegment()), listOf(plan("p1", status = 0)))
                } else {
                    timelineOf(listOf(smartSegment()), listOf(plan("p1", status = 1)))
                }
            }
            val fresh = timelineOf(listOf(smartSegment()), listOf(plan("p1", status = 1)))
            coEvery { fetchTimelineUseCase(any()) } returns fresh
            vm.setScreenVisible(true)

            vm.createSmartRecommendationSegment(addPlanData())
            testDispatcher.scheduler.advanceUntilIdle()

            assertNull(vm.scrollToNewSegmentPlanId.value)
            coVerify(exactly = 1) { fetchTimelineUseCase(any()) }
            coVerify(exactly = 1) {
                viewListener.showAlert(AlertType.INFO, "Your recommendations are ready.")
            }
        }

    @Test
    fun `background success does not show ready notice when screen is not visible`() =
        runTest(testDispatcher) {
            val vm = vm()
            seedTimeline(vm, timelineOf(emptyList(), emptyList()))
            stubPolling { attempt ->
                if (attempt < 25) {
                    timelineOf(listOf(smartSegment()), listOf(plan("p1", status = 0)))
                } else {
                    timelineOf(listOf(smartSegment()), listOf(plan("p1", status = 1)))
                }
            }
            coEvery { fetchTimelineUseCase(any()) } returns
                timelineOf(listOf(smartSegment()), listOf(plan("p1", status = 1)))
            vm.setScreenVisible(false)

            vm.createSmartRecommendationSegment(addPlanData())
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 0) { viewListener.showAlert(AlertType.INFO, "Your recommendations are ready.") }
        }

    @Test
    fun `background failure with negative status deletes the recommendation at its relocated index and reports an error`() =
        runTest(testDispatcher) {
            val vm = vm()
            seedTimeline(vm, timelineOf(emptyList(), emptyList()))
            stubPolling { attempt ->
                if (attempt < 25) {
                    timelineOf(listOf(smartSegment()), listOf(plan("p1", status = 0)))
                } else {
                    timelineOf(listOf(smartSegment()), listOf(plan("p1", status = -1, hasSteps = false)))
                }
            }
            val freshSegments = listOf(smartSegment(title = "other-1"), smartSegment(title = "other-2"), smartSegment())
            val freshPlans = listOf(plan("other-1", 1), plan("other-2", 1), plan("p1", status = -1, hasSteps = false))
            coEvery { fetchTimelineUseCase(any()) } returns timelineOf(freshSegments, freshPlans)

            vm.createSmartRecommendationSegment(addPlanData())
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 1) { deleteSegmentUseCase(DeleteSegmentUseCase.Params("trip-1", 2)) }
            coVerify(exactly = 1) {
                viewListener.showAlert(AlertType.ERROR, "We couldn't generate recommendations right now. Please try again.")
            }
        }

    @Test
    fun `exhausted polling with the recommendation already deleted by the user does not delete again or show an error`() =
        runTest(testDispatcher) {
            val vm = vm()
            seedTimeline(vm, timelineOf(emptyList(), emptyList()))
            stubPolling { timelineOf(listOf(smartSegment()), listOf(plan("p1", status = 0, hasSteps = false))) }
            coEvery { fetchTimelineUseCase(any()) } returns timelineOf(emptyList(), emptyList())

            vm.createSmartRecommendationSegment(addPlanData())
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 0) { deleteSegmentUseCase(any()) }
            coVerify(exactly = 0) { viewListener.showAlert(AlertType.ERROR, any()) }
        }

    @Test
    fun `recommendation plan disappearing from a background poll applies nothing and reports no error`() =
        runTest(testDispatcher) {
            val vm = vm()
            seedTimeline(vm, timelineOf(emptyList(), emptyList()))
            stubPolling { attempt ->
                if (attempt < 25) {
                    timelineOf(listOf(smartSegment()), listOf(plan("p1", status = 0)))
                } else {
                    timelineOf(emptyList(), emptyList())
                }
            }

            vm.createSmartRecommendationSegment(addPlanData())
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 0) { fetchTimelineUseCase(any()) }
            coVerify(exactly = 0) { deleteSegmentUseCase(any()) }
            coVerify(exactly = 0) { viewListener.showAlert(AlertType.ERROR, any()) }
        }

    @Test
    fun `createSmartRecommendationSegment while a generation is pending shows the continues notice and never issues a second PUT`() =
        runTest(testDispatcher) {
            val vm = vm()
            seedTimeline(vm, timelineOf(emptyList(), emptyList()))
            stubPolling { timelineOf(listOf(smartSegment()), listOf(plan("p1", status = 0))) }

            vm.createSmartRecommendationSegment(addPlanData())
            testDispatcher.scheduler.runCurrent()
            assertTrue(vm.isRecommendationGenerationPending)

            vm.createSmartRecommendationSegment(addPlanData())

            coVerify(exactly = 1) { createSegmentUseCase(any()) }
            coVerify(exactly = 1) {
                viewListener.showAlert(AlertType.INFO, "Your recommendations are still being prepared. The list will update automatically when they're ready.")
            }
        }

    @Test
    fun `createSmartRecommendationSegment sends the PUT title built from the injected token`() =
        runTest(testDispatcher) {
            val vm = vm()
            seedTimeline(vm, timelineOf(emptyList(), emptyList()))
            vm.recommendationTitleToken = { "fixed-token" }
            stubPolling { timelineOf(listOf(smartSegment()), listOf(plan("p1", status = 0))) }

            vm.createSmartRecommendationSegment(addPlanData())
            testDispatcher.scheduler.runCurrent()

            val captured = slot<CreateSegmentUseCase.Params>()
            coVerify { createSegmentUseCase(capture(captured)) }
            assertEquals("Recommendations-fixed-token", captured.captured.title)
        }

    @Test
    fun `onReturnFromSavedPlans resumes polling only for the pending smart recommendation plan`() =
        runTest(testDispatcher) {
            val vm = vm()
            every { timelineRepository.consumeGeneratedTimeline(any()) } returns timelineOf(
                segments = listOf(smartSegment(title = "Recommendations-abc"), smartSegment(title = "A regular plan", smart = null)),
                plans = listOf(plan("smart-pending", status = 0), plan("other-pending", status = 0))
            )
            coEvery { waitForGenerationUseCase(any()) } returns timelineOf(emptyList(), emptyList())

            vm.onReturnFromSavedPlans()
            testDispatcher.scheduler.runCurrent()

            coVerify(exactly = 1) {
                waitForGenerationUseCase(withArg<WaitForGenerationUseCase.Params> { })
            }
        }

    @Test
    fun `cleanup delete waits for an in-flight user segment delete and uses the re-located index`() =
        runTest(testDispatcher) {
            val vm = vm()
            val userSegment = TimelineSegment().apply {
                segmentType = SegmentType.MANUAL_POI
                title = "user"
            }
            seedTimeline(vm, timelineOf(mutableListOf(userSegment), mutableListOf(plan("u1", 1))))
            stubPolling {
                timelineOf(
                    listOf(userSegment, smartSegment()),
                    listOf(plan("u1", 1), plan("p1", status = -1, hasSteps = false))
                )
            }
            coEvery { fetchTimelineUseCase(any()) } returns
                timelineOf(listOf(smartSegment()), listOf(plan("p1", status = -1, hasSteps = false)))
            val userDeleteGate = CompletableDeferred<Unit>()
            val deletedIndices = mutableListOf<Int>()
            var inFlight = 0
            var maxInFlight = 0
            coEvery { deleteSegmentUseCase(any()) } coAnswers {
                inFlight++
                maxInFlight = maxOf(maxInFlight, inFlight)
                val isUserDelete = deletedIndices.isEmpty() && !userDeleteGate.isCompleted
                deletedIndices += firstArg<DeleteSegmentUseCase.Params>().segmentIndex
                if (isUserDelete) userDeleteGate.await()
                inFlight--
                ResponseModelBase().apply { status = 200 }
            }

            vm.deleteSegment(0)
            vm.createSmartRecommendationSegment(addPlanData())
            testDispatcher.scheduler.advanceTimeBy(10_000)
            testDispatcher.scheduler.runCurrent()

            assertEquals(listOf(0), deletedIndices)

            userDeleteGate.complete(Unit)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(1, maxInFlight)
            assertEquals(listOf(0, 0), deletedIndices)
            coVerify(exactly = 1) {
                viewListener.showAlert(AlertType.ERROR, "We couldn't generate recommendations right now. Please try again.")
            }
        }
}
