package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.one.api.tour.model.TourProductLookupResponse
import com.tripian.trpcore.base.SuspendUseCase
import com.tripian.trpcore.repository.TourRepository
import javax.inject.Inject

/**
 * Resolve a single tour product by `providerId + productId`. Used when the timeline
 * doesn't carry coordinates / city for an activity (e.g. no-location segments).
 *
 * Backed by `GET /tour-api/product-lookup`.
 */
class LookupTourProductUseCase @Inject constructor(
    private val repository: TourRepository
) : SuspendUseCase<TourProductLookupResponse, LookupTourProductUseCase.Params>() {

    data class Params(
        val providerId: Int,
        val productId: String
    )

    override suspend fun execute(params: Params): TourProductLookupResponse =
        repository.lookupTourProductAsync(
            providerId = params.providerId,
            productId = params.productId
        )
}
