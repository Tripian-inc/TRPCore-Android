package com.tripian.trpcore.ui.timeline.activity

import android.os.Looper
import com.tripian.one.api.cities.model.City
import com.tripian.one.api.tour.model.TourFacet
import com.tripian.one.api.tour.model.TourFacetDurationRange
import com.tripian.one.api.tour.model.TourFacetMoney
import com.tripian.one.api.tour.model.TourFacetPriceRange
import com.tripian.one.api.tour.model.TourProduct
import com.tripian.one.api.tour.model.TourSearchData
import com.tripian.one.api.tour.model.TourSearchResponse
import com.tripian.trpcore.base.AppConfig
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.domain.model.timeline.AddPlanData
import com.tripian.trpcore.domain.model.timeline.SortOption
import com.tripian.trpcore.domain.usecase.timeline.CreateReservedActivitySegmentUseCase
import com.tripian.trpcore.domain.usecase.timeline.FetchTimelineUseCase
import com.tripian.trpcore.domain.usecase.timeline.SearchToursUseCase
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.ui.timeline.compose.activity.needsListingInitialization
import com.tripian.trpcore.ui.timeline.compose.nav.ActivityListingArgs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pins [ACActivityListingVM.initialize]'s reset-on-re-entry contract (filter, sort,
 * search and category state must not leak from a previous listing into a new one)
 * and [ACActivityListingVM] facet publishing, plus the
 * [needsListingInitialization] guard that decides whether a Compose re-entry should
 * re-run [ACActivityListingVM.initialize].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ACActivityListingVMTest {

    private val searchToursUseCase: SearchToursUseCase = mockk()
    private val createReservedActivitySegmentUseCase: CreateReservedActivitySegmentUseCase =
        mockk(relaxed = true)
    private val fetchTimelineUseCase: FetchTimelineUseCase = mockk(relaxed = true)
    private val timelineRepository: TimelineRepository = mockk(relaxed = true)
    private val mainLooper: Looper = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockkStatic(Looper::class)
        every { Looper.getMainLooper() } returns mainLooper
        every { Looper.myLooper() } returns mainLooper
        every { mainLooper.thread } returns Thread.currentThread()

        val appConfig: AppConfig = mockk(relaxed = true)
        every { appConfig.appCurrency } returns "USD"
        val core: TRPCore = mockk()
        every { core.appConfig } returns appConfig
        TRPCore.core = core
    }

    @After
    fun tearDown() {
        unmockkStatic(Looper::class)
        Dispatchers.resetMain()
    }

    private fun vm(): ACActivityListingVM = ACActivityListingVM(
        searchToursUseCase = searchToursUseCase,
        createReservedActivitySegmentUseCase = createReservedActivitySegmentUseCase,
        fetchTimelineUseCase = fetchTimelineUseCase,
        timelineRepository = timelineRepository
    )

    private fun city(id: Int = 109): City {
        val city: City = mockk(relaxed = true)
        every { city.id } returns id
        return city
    }

    private fun planData(cityId: Int = 109, dayIndex: Int = 0): AddPlanData =
        AddPlanData(
            selectedCity = city(cityId),
            selectedDayIndex = dayIndex,
            travelers = 2
        )

    private fun emptyResponse(): TourSearchResponse {
        val response: TourSearchResponse = mockk(relaxed = true)
        every { response.data } returns TourSearchData().apply {
            products = emptyList()
            total = 0
            facets = emptyList()
        }
        return response
    }

    @Test
    fun `test second initialize resets filter sort and search request success`() = runTest {
        coEvery { searchToursUseCase(any()) } returns emptyResponse()

        val vm = vm()
        vm.initialize(planData = planData(), tripHash = "hash-1")

        vm.applyFilter(vm.getCurrentFilter().copy(maxDuration = 0f))
        vm.applySort(SortOption.RATING)

        assertEquals(0f, vm.getCurrentFilter().maxDuration)
        assertEquals(SortOption.RATING, vm.getCurrentSort())

        val paramsSlot = slot<SearchToursUseCase.Params>()
        coEvery { searchToursUseCase(capture(paramsSlot)) } returns emptyResponse()

        vm.initialize(planData = planData(), tripHash = "hash-2")

        assertEquals(ActivityFilterData.default(), vm.getCurrentFilter())
        assertEquals(SortOption.POPULARITY, vm.getCurrentSort())

        val params = paramsSlot.captured
        assertNull(params.minDuration)
        assertNull(params.maxDuration)
        assertNull(params.maxPrice)
        assertEquals(1, params.minPrice)
        assertNull(params.keywords)
        assertEquals(SortOption.POPULARITY.sortingBy, params.sortingBy)
        assertEquals(SortOption.POPULARITY.sortingType, params.sortingType)
    }

    private fun responseWithDurationRange(min: Int, max: Int): TourSearchResponse {
        val range = TourFacetDurationRange().apply {
            minimumMinutes = min
            maximumMinutes = max
        }
        val facet = TourFacet().apply { durationRange = range }
        return TourSearchResponse().apply {
            data = TourSearchData().apply {
                products = emptyList()
                total = 0
                facets = listOf(facet)
            }
        }
    }

    @Test
    fun `test equal duration facet bounds publish null durationRangeFacet success`() = runTest {
        coEvery { searchToursUseCase(any()) } returns responseWithDurationRange(min = 0, max = 0)

        val vm = vm()
        vm.initialize(planData = planData(), tripHash = "hash")

        assertNull(vm.durationRangeFacet.value)
    }

    @Test
    fun `test wider duration facet bounds publish durationRangeFacet success`() = runTest {
        coEvery { searchToursUseCase(any()) } returns responseWithDurationRange(min = 30, max = 300)

        val vm = vm()
        vm.initialize(planData = planData(), tripHash = "hash")

        assertEquals(30, vm.durationRangeFacet.value?.minimumMinutes)
        assertEquals(300, vm.durationRangeFacet.value?.maximumMinutes)
    }

    private fun responseWithPriceRange(min: Int?, max: Int?): TourSearchResponse {
        val priceRange = TourFacetPriceRange().apply {
            minimum = min?.let { TourFacetMoney().apply { amount = it } }
            maximum = max?.let { TourFacetMoney().apply { amount = it } }
        }
        val facet = TourFacet().apply { this.priceRange = priceRange }
        return TourSearchResponse().apply {
            data = TourSearchData().apply {
                products = emptyList()
                total = 0
                facets = listOf(facet)
            }
        }
    }

    @Test
    fun `test equal price facet bounds publish null priceRangeFacet success`() = runTest {
        coEvery { searchToursUseCase(any()) } returns responseWithPriceRange(min = 50, max = 50)

        val vm = vm()
        vm.initialize(planData = planData(), tripHash = "hash")

        assertNull(vm.priceRangeFacet.value)
    }

    @Test
    fun `test wider price facet bounds publish priceRangeFacet success`() = runTest {
        coEvery { searchToursUseCase(any()) } returns responseWithPriceRange(min = 10, max = 200)

        val vm = vm()
        vm.initialize(planData = planData(), tripHash = "hash")

        assertEquals(10, vm.priceRangeFacet.value?.minimum?.amount)
        assertEquals(200, vm.priceRangeFacet.value?.maximum?.amount)
    }

    @Test
    fun `test missing price facet amount publishes null priceRangeFacet success`() = runTest {
        coEvery { searchToursUseCase(any()) } returns responseWithPriceRange(min = null, max = 200)

        val vm = vm()
        vm.initialize(planData = planData(), tripHash = "hash")

        assertNull(vm.priceRangeFacet.value)
    }

    @Test
    fun `test needsListingInitialization is true for a null previous args`() {
        val args = ActivityListingArgs(
            planData = planData(),
            tripHash = "hash",
            plannedActivityIdsByDay = emptyMap(),
            tripWideExcludedActivityIds = emptyList()
        )

        assertTrue(needsListingInitialization(null, args))
    }

    @Test
    fun `test needsListingInitialization is false for an equal args instance`() {
        val args = ActivityListingArgs(
            planData = planData(),
            tripHash = "hash",
            plannedActivityIdsByDay = emptyMap(),
            tripWideExcludedActivityIds = emptyList()
        )

        assertFalse(needsListingInitialization(args, args))
    }

    @Test
    fun `test needsListingInitialization is true for a new args instance with equal values`() {
        val plan = planData()
        val previous = ActivityListingArgs(
            planData = plan,
            tripHash = "hash",
            plannedActivityIdsByDay = emptyMap(),
            tripWideExcludedActivityIds = emptyList()
        )
        val incoming = ActivityListingArgs(
            planData = plan,
            tripHash = "hash",
            plannedActivityIdsByDay = emptyMap(),
            tripWideExcludedActivityIds = emptyList()
        )

        assertTrue(needsListingInitialization(previous, incoming))
    }

    @Test
    fun `test needsListingInitialization is false for the same instance after its planData mutated`() {
        val args = ActivityListingArgs(
            planData = planData(dayIndex = 0),
            tripHash = "hash",
            plannedActivityIdsByDay = emptyMap(),
            tripWideExcludedActivityIds = emptyList()
        )
        args.planData.selectedDayIndex = 2

        assertFalse(needsListingInitialization(args, args))
    }

    @Test
    fun `test needsListingInitialization is true when planData differs`() {
        val previous = ActivityListingArgs(
            planData = planData(dayIndex = 0),
            tripHash = "hash",
            plannedActivityIdsByDay = emptyMap(),
            tripWideExcludedActivityIds = emptyList()
        )
        val incoming = ActivityListingArgs(
            planData = planData(dayIndex = 1),
            tripHash = "hash",
            plannedActivityIdsByDay = emptyMap(),
            tripWideExcludedActivityIds = emptyList()
        )

        assertTrue(needsListingInitialization(previous, incoming))
    }
}
