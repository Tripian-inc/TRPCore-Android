package com.tripian.trpcore.ui.timeline.activity

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import com.tripian.one.api.cities.model.City
import com.tripian.one.api.pois.model.Coordinate
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
import com.tripian.trpcore.util.Preferences
import com.tripian.trpcore.util.Strings
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Pins [ACActivityListingVM]'s server-side-only search/filter/sort contract:
 * every call to [ACActivityListingVM.search], [ACActivityListingVM.applyFilter] and
 * [ACActivityListingVM.applySort] goes through [SearchToursUseCase] with offset reset
 * to 0, and the request carries keywords/min-max/sort straight from the current state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ACActivityListingVMTest {

    private val searchToursUseCase: SearchToursUseCase = mockk()
    private val capturedParams = mutableListOf<SearchToursUseCase.Params>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) = runnable.run()
            override fun postToMainThread(runnable: Runnable) = runnable.run()
            override fun isMainThread(): Boolean = true
        })

        val appConfig: AppConfig = mockk(relaxed = true)
        every { appConfig.appCurrency } returns "EUR"
        val core: TRPCore = mockk(relaxed = true)
        every { core.appConfig } returns appConfig
        TRPCore.core = core

        capturedParams.clear()
    }

    @After
    fun tearDown() {
        ArchTaskExecutor.getInstance().setDelegate(null)
        Dispatchers.resetMain()
    }

    private fun stubResponse(products: List<TourProduct>, total: Int) {
        coEvery { searchToursUseCase(any()) } coAnswers {
            @Suppress("UNCHECKED_CAST")
            val params = it.invocation.args[0] as SearchToursUseCase.Params
            capturedParams += params
            TourSearchResponse().apply {
                data = TourSearchData().apply {
                    this.products = products
                    this.total = total
                }
            }
        }
    }

    private fun product(id: String): TourProduct = TourProduct().apply { productId = id }

    private fun city(id: Int): City {
        val c: City = mockk(relaxed = true)
        every { c.id } returns id
        every { c.coordinate } returns Coordinate().apply { lat = 41.0; lng = 2.0 }
        every { c.timezone } returns null
        return c
    }

    private fun planData(cityId: Int = 109): AddPlanData = AddPlanData(
        selectedDay = Date(),
        selectedDayIndex = 0,
        selectedCity = city(cityId),
        travelers = 1
    )

    private fun vm(): ACActivityListingVM {
        val vm = ACActivityListingVM(
            searchToursUseCase = searchToursUseCase,
            createReservedActivitySegmentUseCase = mockk<CreateReservedActivitySegmentUseCase>(relaxed = true),
            fetchTimelineUseCase = mockk<FetchTimelineUseCase>(relaxed = true),
            timelineRepository = mockk<TimelineRepository>(relaxed = true)
        )
        vm.strings = mockk<Strings>(relaxed = true)
        vm.miscRepository = mockk<com.tripian.trpcore.repository.MiscRepository>(relaxed = true)
        return vm
    }

    @Test
    fun `test initial load sends base request with no search filter or sort success`() = runTest {
        stubResponse(listOf(product("1"), product("2"), product("3")), total = 3)
        val vm = vm()

        vm.initialize(planData(), tripHash = "trip-1")

        assertEquals(1, capturedParams.size)
        val params = capturedParams[0]
        assertNull(params.keywords)
        assertEquals(1, params.minPrice)
        assertNull(params.maxPrice)
        assertNull(params.minDuration)
        assertNull(params.maxDuration)
        assertEquals("score", params.sortingBy)
        assertEquals("desc", params.sortingType)
        assertEquals(0, params.offset)
        assertEquals(10, params.limit)
    }

    @Test
    fun `test initial load fully loads a city whose total fits one page success`() = runTest {
        val products = listOf(product("1"), product("2"), product("3"))
        stubResponse(products, total = 3)
        val vm = vm()

        vm.initialize(planData(), tripHash = "trip-1")

        assertEquals(3, vm.activities.value?.size)
        assertEquals(3, vm.activityCount.value)
    }

    @Test
    fun `test search with non-blank query sends keywords to api success`() = runTest {
        stubResponse(emptyList(), total = 0)
        val vm = vm()
        vm.initialize(planData(), tripHash = "trip-1")

        vm.search("museum")

        assertEquals(2, capturedParams.size)
        assertEquals("museum", capturedParams[1].keywords)
    }

    @Test
    fun `test search trims whitespace from query before sending keywords success`() = runTest {
        stubResponse(emptyList(), total = 0)
        val vm = vm()
        vm.initialize(planData(), tripHash = "trip-1")

        vm.search("  museum  ")

        assertEquals(2, capturedParams.size)
        assertEquals("museum", capturedParams[1].keywords)
    }

    @Test
    fun `test search with identical query does not trigger another api call success`() = runTest {
        stubResponse(emptyList(), total = 0)
        val vm = vm()
        vm.initialize(planData(), tripHash = "trip-1")

        vm.search("museum")
        vm.search("museum")

        assertEquals(2, capturedParams.size)
    }

    @Test
    fun `test search with blank query resets to base request success`() = runTest {
        stubResponse(emptyList(), total = 0)
        val vm = vm()
        vm.initialize(planData(), tripHash = "trip-1")
        vm.search("museum")

        vm.search("")

        assertEquals(3, capturedParams.size)
        val params = capturedParams[2]
        assertNull(params.keywords)
        assertEquals(1, params.minPrice)
        assertNull(params.maxPrice)
        assertEquals("score", params.sortingBy)
        assertEquals(0, params.offset)
    }

    @Test
    fun `test applyFilter sends filter bounds to api success`() = runTest {
        stubResponse(emptyList(), total = 0)
        val vm = vm()
        vm.initialize(planData(), tripHash = "trip-1")

        vm.applyFilter(
            ActivityFilterData(minPrice = 50f, maxPrice = 200f, minDuration = 30f, maxDuration = 120f)
        )

        assertEquals(2, capturedParams.size)
        val params = capturedParams[1]
        assertEquals(50, params.minPrice)
        assertEquals(200, params.maxPrice)
        assertEquals(30, params.minDuration)
        assertEquals(120, params.maxDuration)
        assertEquals(0, params.offset)
    }

    @Test
    fun `test applySort sends sort params to api success`() = runTest {
        stubResponse(emptyList(), total = 0)
        val vm = vm()
        vm.initialize(planData(), tripHash = "trip-1")

        vm.applySort(SortOption.PRICE_LOW_TO_HIGH)

        assertEquals(2, capturedParams.size)
        val params = capturedParams[1]
        assertEquals("price", params.sortingBy)
        assertEquals("asc", params.sortingType)
        assertEquals(0, params.offset)
    }

    @Test
    fun `test search resets offset to zero after loading more pages success`() = runTest {
        val firstPage = (1..10).map { product(it.toString()) }
        stubResponse(firstPage, total = 25)
        val vm = vm()
        vm.initialize(planData(), tripHash = "trip-1")

        vm.loadMoreActivities()
        assertEquals(2, capturedParams.size)
        assertEquals(10, capturedParams[1].offset)

        vm.search("museum")

        assertEquals(3, capturedParams.size)
        assertEquals(0, capturedParams[2].offset)
    }
}
