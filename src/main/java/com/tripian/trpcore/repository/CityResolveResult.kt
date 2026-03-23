package com.tripian.trpcore.repository

import com.tripian.one.api.cities.model.City

/**
 * Result type for city resolution API calls.
 * Handles three scenarios:
 * - Success: All cities resolved successfully
 * - PartialSuccess: Some cities resolved, some not found (cityId = 0)
 * - AllFailed: No cities could be resolved
 */
sealed class CityResolveResult {
    /**
     * All requested cities were successfully resolved
     */
    data class Success(val cities: List<City>) : CityResolveResult()

    /**
     * Some cities were resolved, but others were not found in the database.
     * SDK should continue with resolved cities and show a warning.
     */
    data class PartialSuccess(
        val cities: List<City>,
        val unresolvedCityNames: List<String>
    ) : CityResolveResult()

    /**
     * No cities could be resolved (all returned cityId = 0 or null).
     * SDK should show error and close.
     */
    data class AllFailed(val unresolvedCityNames: List<String>) : CityResolveResult()
}
