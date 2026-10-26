package com.tripian.trpcore.ui.timeline.activity

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import com.tripian.one.api.cities.model.City
import com.tripian.one.api.pois.model.Coordinate
import com.tripian.one.api.tour.model.TourFacet
import com.tripian.one.api.tour.model.TourFacetCategory
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
import com.tripian.trpcore.util.Preferences
import com.tripian.trpcore.util.Strings
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.util.Date
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pins [ACActivityListingVM]'s server-side search/filter/sort requests, the category
 * strip rebuilt from unfiltered first-page responses, facet range publishing, the
 * reset-on-re-entry contract of [ACActivityListingVM.initialize] and the
 * [needsListingInitialization] guard.
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

    private fun planData(cityId: Int = 109, dayIndex: Int = 0): AddPlanData = AddPlanData(
        selectedDay = Date(),
        selectedDayIndex = dayIndex,
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

    @Test
    fun `test stale all response arriving after newer filtered request does not rebuild category strip success`() = runTest {
        val staleResponse = CompletableDeferred<TourSearchResponse>()
        val initial = response(facets = listOf(TourFacet().apply {
            categories = listOf(facetCategory("c1", "Museums"), facetCategory("c2", "Tours"))
        }))
        val filtered = response(facets = listOf(TourFacet().apply { categories = listOf(facetCategory("c9", "Other")) }))
        var call = 0
        coEvery { searchToursUseCase(any()) } coAnswers {
            capturedParams += it.invocation.args[0] as SearchToursUseCase.Params
            when (call++) {
                0 -> initial
                1 -> withContext(NonCancellable) { staleResponse.await() }
                else -> filtered
            }
        }
        val vm = vm()
        vm.initialize(planData(), tripHash = "trip-1")

        vm.loadActivities()
        vm.onCategorySelectionChanged(setOf(1))
        staleResponse.complete(
            response(facets = listOf(TourFacet().apply { categories = listOf(facetCategory("c5", "Stale")) }))
        )

        assertEquals(3, capturedParams.size)
        assertNull(capturedParams[1].categoryIds)
        assertEquals("c1", capturedParams[2].categoryIds)
        assertEquals(listOf("c1", "c2"), vm.getFacetCategoryItems().drop(1).map { it.id })
        assertEquals(setOf(1), vm.selectedCategoryIndices.value)
    }

    @Test
    fun `test rebuild re-emits selected category indices success`() = runTest {
        stubResponses(
            response(facets = listOf(TourFacet().apply { categories = listOf(facetCategory("c1", "Museums")) })),
            response(facets = listOf(TourFacet().apply {
                categories = listOf(facetCategory("c2", "Tours"), facetCategory("c3", "Food"))
            }))
        )
        val vm = vm()
        vm.initialize(planData(), tripHash = "trip-1")
        val emissions = mutableListOf<Set<Int>>()
        vm.selectedCategoryIndices.observeForever { emissions += it }
        emissions.clear()

        vm.loadActivities()

        assertEquals(listOf(setOf(0)), emissions)
    }

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

    @Test
    fun `test second initialize after category selection sends null category ids and rebuilds strip success`() = runTest {
        stubResponses(
            response(facets = listOf(TourFacet().apply {
                categories = listOf(facetCategory("c1", "Museums"), facetCategory("c2", "Tours"))
            })),
            response(facets = listOf(TourFacet().apply { categories = listOf(facetCategory("c9", "Other")) })),
            response(facets = listOf(TourFacet().apply {
                categories = listOf(facetCategory("c7", "Food"), facetCategory("c8", "Nightlife"))
            }))
        )
        val vm = vm()
        vm.initialize(planData(), tripHash = "trip-1")
        vm.onCategorySelectionChanged(setOf(1))
        assertEquals("c1", capturedParams.last().categoryIds)
        val requestsBeforeReinit = capturedParams.size

        vm.initialize(planData(), tripHash = "trip-2")

        assertEquals(requestsBeforeReinit + 1, capturedParams.size)
        assertNull(capturedParams[requestsBeforeReinit].categoryIds)
        assertEquals(listOf("c7", "c8"), vm.getFacetCategoryItems().drop(1).map { it.id })
        assertEquals(setOf(0), vm.selectedCategoryIndices.value)
    }
}
