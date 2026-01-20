package com.tripian.trpcore.domain.manager

import com.tripian.trpcore.repository.PoiRepository
import com.tripian.trpcore.ui.timeline.poilisting.POIListingType
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

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
    private var fetchDisposable: Disposable? = null

    /**
     * Category IDs that identify "Eat & Drink" groups
     * If ANY category in a group has one of these IDs, the entire group is considered Eat & Drink
     * Based on API documentation: Restaurants (3), Cafes (4), Bars/Pubs (24)
     */
    private val EAT_AND_DRINK_CATEGORY_IDS = listOf(3, 4, 24)

    /**
     * Prefetch categories if not already fetched
     * Uses lazy loading - only called when POI listing is first accessed
     *
     * @param repository PoiRepository instance for API call
     * @param onComplete Callback when prefetch completes (success or failure)
     */
    fun prefetchIfNeeded(
        repository: PoiRepository,
        onComplete: () -> Unit = {}
    ) {
        // Already fetched, call completion immediately
        if (isCategoriesFetched) {
            onComplete()
            return
        }

        // Currently fetching, don't start another request
        if (isFetching) {
            return
        }

        isFetching = true

        fetchDisposable = repository.getPoiCategories()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                val categoryModel = response.data
                val groups = categoryModel?.groups ?: emptyList()
                val categories = categoryModel?.categories ?: emptyList()

                // Collect all category IDs
                allCategoryIds = categories.map { it.id }

                // Separate categories by group
                // Check if ANY category in a group has ID 3, 4, or 24
                // If so, include ALL categories from that group in Eat & Drink
                if (groups.isNotEmpty()) {
                    val eatDrinkCategories = mutableListOf<Int>()
                    val poiCategories = mutableListOf<Int>()

                    groups.forEach { group ->
                        val groupCategoryIds = group.categories?.map { it.id } ?: emptyList()

                        // Check if ANY category in this group has ID 3, 4, or 24
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
                    // Fallback: No groups available, return empty lists
                    eatAndDrinkCategoryIds = emptyList()
                    placesOfInterestCategoryIds = emptyList()
                }

                isCategoriesFetched = true
                isFetching = false
                onComplete()
            }, { _ ->
                // On error, return empty lists
                eatAndDrinkCategoryIds = emptyList()
                placesOfInterestCategoryIds = emptyList()
                isFetching = false
                onComplete()
            })
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

    /**
     * Check if categories have been fetched
     */
    fun isReady(): Boolean = isCategoriesFetched

    /**
     * Clear cached categories
     * Used for testing or when user logs out
     */
    fun clear() {
        fetchDisposable?.dispose()
        fetchDisposable = null
        allCategoryIds = emptyList()
        eatAndDrinkCategoryIds = emptyList()
        placesOfInterestCategoryIds = emptyList()
        isCategoriesFetched = false
        isFetching = false
    }

    /**
     * Get all category IDs
     */
    fun getAllCategoryIds(): List<Int> = allCategoryIds
}
