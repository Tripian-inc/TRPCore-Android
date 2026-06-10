package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.one.api.cities.model.CityResolveData
import com.tripian.one.api.pois.model.Coordinate
import com.tripian.trpcore.base.SuspendUseCase
import com.tripian.trpcore.repository.TimelineRepository
import javax.inject.Inject

class ResolveCitiesUseCase @Inject constructor(
    private val repository: TimelineRepository
) : SuspendUseCase<List<CityResolveData>, ResolveCitiesUseCase.Params>() {

    data class Params(val coordinates: List<Coordinate>)

    override suspend fun execute(params: Params): List<CityResolveData> =
        repository.resolveCitiesAsync(params.coordinates)
}
