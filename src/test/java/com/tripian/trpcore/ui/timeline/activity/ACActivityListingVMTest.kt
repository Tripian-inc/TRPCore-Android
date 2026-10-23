package com.tripian.trpcore.ui.timeline.activity

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import com.tripian.one.api.cities.model.City
import com.tripian.one.api.pois.model.Coordinate
import com.tripian.one.api.tour.model.TourFacet
import com.tripian.one.api.tour.model.TourFacetCategory
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
        stubResponses(response(products, total))
    }

    private fun response(
        products: List<TourProduct> = emptyList(),
        total: Int = 0,
        facets: List<TourFacet>? = null
    ): TourSearchResponse = TourSearchResponse().apply {
        data = TourSearchData().apply {
            this.products = products
            this.total = total
            this.facets = facets
        }
    }

    private fun facetCategory(id: String, label: String): TourFacetCategory =
        TourFacetCategory().apply {
            this.id = id
            this.label = label
        }

    /** Scripts one [TourSearchResponse] per call; the last one repeats once exhausted. */
    private fun stubResponses(vararg responses: TourSearchResponse) {
        val queue = responses.toMutableList()
        coEvery { searchToursUseCase(any()) } coAnswers {
            @Suppress("UNCHECKED_CAST")
            val params = it.invocation.args[0] as SearchToursUseCase.Params
            capturedParams += params
            if (queue.size > 1) queue.removeAt(0) else queue.first()
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

    @Test
    fun `test second page zero response with all selected replaces category strip success`() = runTest {
        stubResponses(
            response(facets = listOf(TourFacet().apply { categories = listOf(facetCategory("c1", "Museums")) })),
            response(facets = listOf(TourFacet().apply {
                categories = listOf(facetCategory("c2", "Tours"), facetCategory("c3", "Food"))
            }))
        )
        val vm = vm()
        vm.initialize(planData(), tripHash = "trip-1")
        assertEquals(listOf("c1"), vm.getFacetCategoryItems().drop(1).map { it.id })

        vm.loadActivities()

        assertEquals(listOf("c2", "c3"), vm.getFacetCategoryItems().drop(1).map { it.id })
    }

    @Test
    fun `test response for a selected category does not change category strip success`() = runTest {
        stubResponses(
            response(facets = listOf(TourFacet().apply {
                categories = listOf(facetCategory("c1", "Museums"), facetCategory("c2", "Tours"))
            })),
            response(facets = listOf(TourFacet().apply { categories = listOf(facetCategory("c9", "Other")) }))
        )
        val vm = vm()
        vm.initialize(planData(), tripHash = "trip-1")

        vm.onCategorySelectionChanged(setOf(1))

        assertEquals("c1", capturedParams.last().categoryIds)
        assertEquals(listOf("c1", "c2"), vm.getFacetCategoryItems().drop(1).map { it.id })
    }

    @Test
    fun `test empty facets list keeps previous category strip success`() = runTest {
        stubResponses(
            response(facets = listOf(TourFacet().apply { categories = listOf(facetCategory("c1", "Museums")) })),
            response(facets = emptyList())
        )
        val vm = vm()
        vm.initialize(planData(), tripHash = "trip-1")

        vm.loadActivities()

        assertEquals(listOf("c1"), vm.getFacetCategoryItems().drop(1).map { it.id })
    }

    @Test
    fun `test facet with empty categories keeps previous category strip success`() = runTest {
        stubResponses(
            response(facets = listOf(TourFacet().apply { categories = listOf(facetCategory("c1", "Museums")) })),
            response(facets = listOf(TourFacet().apply { categories = emptyList() }))
        )
        val vm = vm()
        vm.initialize(planData(), tripHash = "trip-1")

        vm.loadActivities()

        assertEquals(listOf("c1"), vm.getFacetCategoryItems().drop(1).map { it.id })
    }

    @Test
    fun `test pagination response never rebuilds category strip success`() = runTest {
        val firstPage = (1..10).map { product(it.toString()) }
        val secondPage = (11..20).map { product(it.toString()) }
        stubResponses(
            response(
                products = firstPage,
                total = 25,
                facets = listOf(TourFacet().apply { categories = listOf(facetCategory("c1", "Museums")) })
            ),
            response(
                products = secondPage,
                total = 25,
                facets = listOf(TourFacet().apply { categories = listOf(facetCategory("c9", "Other")) })
            )
        )
        val vm = vm()
        vm.initialize(planData(), tripHash = "trip-1")

        vm.loadMoreActivities()

        assertEquals(listOf("c1"), vm.getFacetCategoryItems().drop(1).map { it.id })
    }

    @Test
    fun `test reconcileCategorySelection drops ids no longer available success`() {
        val result = ACActivityListingVM.reconcileCategorySelection(
            selectedIds = setOf("c1", "c2"),
            availableIds = listOf("c2", "c3")
        )

        assertEquals(setOf("c2"), result)
    }

    @Test
    fun `test reconcileCategorySelection returns empty when none remain success`() {
        val result = ACActivityListingVM.reconcileCategorySelection(
            selectedIds = setOf("c1"),
            availableIds = listOf("c9")
        )

        assertEquals(emptySet<String>(), result)
    }

    @Test
    fun `test selecting single category sends its id success`() = runTest {
        stubResponses(response(facets = listOf(TourFacet().apply {
            categories = listOf(facetCategory("c1", "Museums"), facetCategory("c2", "Tours"))
        })))
        val vm = vm()
        vm.initialize(planData(), tripHash = "trip-1")

        vm.onCategorySelectionChanged(setOf(1))

        assertEquals("c1", capturedParams.last().categoryIds)
        assertEquals(setOf(1), vm.selectedCategoryIndices.value)
    }

    @Test
    fun `test selecting multiple categories sends comma separated ids in selection order success`() = runTest {
        stubResponses(response(facets = listOf(TourFacet().apply {
            categories = listOf(facetCategory("c1", "Museums"), facetCategory("c2", "Tours"))
        })))
        val vm = vm()
        vm.initialize(planData(), tripHash = "trip-1")

        vm.onCategorySelectionChanged(setOf(2, 1))

        assertEquals("c2,c1", capturedParams.last().categoryIds)
        assertEquals(setOf(1, 2), vm.selectedCategoryIndices.value)
    }

    @Test
    fun `test selecting the all chip sends null category ids success`() = runTest {
        stubResponses(response(facets = listOf(TourFacet().apply {
            categories = listOf(facetCategory("c1", "Museums"))
        })))
        val vm = vm()
        vm.initialize(planData(), tripHash = "trip-1")
        vm.onCategorySelectionChanged(setOf(1))

        vm.onCategorySelectionChanged(setOf(0))

        assertNull(capturedParams.last().categoryIds)
        assertEquals(setOf(0), vm.selectedCategoryIndices.value)
    }
}
