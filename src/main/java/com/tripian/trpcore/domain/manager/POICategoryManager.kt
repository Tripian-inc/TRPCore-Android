package com.tripian.trpcore.domain.manager

import com.tripian.trpcore.repository.PoiRepository
import com.tripian.trpcore.ui.timeline.poilisting.POIListingType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * POICategoryManager
 * Singleton manager for POI category ID caching and lazy prefetch
 *
 * Categorizes POIs into two groups:
 * - Eat & Drink: Restaurants, cafes, bars (group IDs: 3, 4, 24)
 * - Places of Interest: All other POI categories
 *
 * Called on first POI listing access (lazy initialization)
 */
object POICategoryManager {

    private var allCategoryIds: List<Int> = emptyList()
    private var eatAndDrinkCategoryIds: List<Int> = emptyList()
    private var placesOfInterestCategoryIds: List<Int> = emptyList()
    private var isCategoriesFetched: Boolean = false
    private var isFetching: Boolean = false
    private var fetchJob: Job? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Category IDs that identify "Eat & Drink" groups
     * If ANY category in a group has one of these IDs, the entire group is considered Eat & Drink
     * Based on API documentation: Restaurants (3), Cafes (4), Bars/Pubs (24)
     */
    private val EAT_AND_DRINK_CATEGORY_IDS = listOf(3, 4, 24)

    /**
     * Prefetch categories if not already fetched. Uses lazy loading — only
     * runs the first time a POI listing is opened. The completion callback
     * is delivered on the main thread.
     */
    fun prefetchIfNeeded(
        repository: PoiRepository,
        onComplete: () -> Unit = {}
    ) {
        if (isCategoriesFetched) {
            onComplete()
            return
        }
        if (isFetching) return
        isFetching = true

        fetchJob = scope.launch {
            try {
                val response = repository.getPoiCategoriesAsync()
                val categoryModel = response.data
                val groups = categoryModel?.groups ?: emptyList()
                val categories = categoryModel?.categories ?: emptyList()

                allCategoryIds = categories.map { it.id }

                if (groups.isNotEmpty()) {
                    val eatDrinkCategories = mutableListOf<Int>()
                    val poiCategories = mutableListOf<Int>()
                    groups.forEach { group ->
                        val groupCategoryIds = group.categories?.map { it.id } ?: emptyList()
                        val isEatAndDrinkGroup = groupCategoryIds.any { categoryId ->
                            EAT_AND_DRINK_CATEGORY_IDS.contains(categoryId)
                        }
                        if (isEatAndDrinkGroup) {
                            eatDrinkCategories.addAll(groupCategoryIds)
                        } else {
                            poiCategories.addAll(groupCategoryIds)
                        }
                    }
                    eatAndDrinkCategoryIds = eatDrinkCategories
                    placesOfInterestCategoryIds = poiCategories
                } else {
                    eatAndDrinkCategoryIds = emptyList()
                    placesOfInterestCategoryIds = emptyList()
                }

                isCategoriesFetched = true
            } catch (_: Throwable) {
                eatAndDrinkCategoryIds = emptyList()
                placesOfInterestCategoryIds = emptyList()
            } finally {
                isFetching = false
                withContext(Dispatchers.Main) { onComplete() }
            }
        }
    }

    /**
     * Get category IDs for the specified listing type
     *
     * @param type POIListingType (PLACES_OF_INTEREST or EAT_AND_DRINK)
     * @return List of category IDs, or null if categories not yet fetched
     */
    fun getCategoryIds(type: POIListingType): List<Int>? {
        if (!isCategoriesFetched) return null

        return when (type) {
            POIListingType.EAT_AND_DRINK ->
                eatAndDrinkCategoryIds.takeIf { it.isNotEmpty() }
            POIListingType.PLACES_OF_INTEREST ->
                placesOfInterestCategoryIds.takeIf { it.isNotEmpty() }
        }
    }

    fun isReady(): Boolean = isCategoriesFetched

    fun clear() {
        fetchJob?.cancel()
        fetchJob = null
        allCategoryIds = emptyList()
        eatAndDrinkCategoryIds = emptyList()
        placesOfInterestCategoryIds = emptyList()
        isCategoriesFetched = false
        isFetching = false
    }

    fun getAllCategoryIds(): List<Int> = allCategoryIds
}
