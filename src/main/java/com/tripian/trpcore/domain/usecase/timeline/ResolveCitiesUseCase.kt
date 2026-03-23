package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.one.api.cities.model.CityResolveData
import com.tripian.one.api.pois.model.Coordinate
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.TimelineRepository
import javax.inject.Inject

/**
 * ResolveCitiesUseCase
 * Resolves city IDs from coordinates by calling cities/resolve API.
 * Used before creating timeline to validate if destinations are supported.
 */
class ResolveCitiesUseCase @Inject constructor(
    private val repository: TimelineRepository
) : BaseUseCase<List<CityResolveData>, ResolveCitiesUseCase.Params>() {

    data class Params(
        val coordinates: List<Coordinate>
    )

    override fun on(params: Params?) {
        params?.let {
            addObservable {
                repository.resolveCities(it.coordinates)
            }
        }
    }
}
