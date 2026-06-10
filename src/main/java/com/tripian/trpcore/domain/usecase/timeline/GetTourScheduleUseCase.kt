package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.one.api.tour.model.TourScheduleResponse
import com.tripian.trpcore.base.SuspendUseCase
import com.tripian.trpcore.repository.TourRepository
import javax.inject.Inject

/**
 * GetTourScheduleUseCase
 * Gets available time slots for a tour on a specific date
 */
class GetTourScheduleUseCase @Inject constructor(
    private val repository: TourRepository
) : SuspendUseCase<TourScheduleResponse, GetTourScheduleUseCase.Params>() {

    data class Params(
        val productId: String,
        val date: String,             // Format: "YYYY-MM-DD" (range start when `to` is set)
        val to: String? = null,       // Optional range end "YYYY-MM-DD"
        val currency: String? = null
    )

    override suspend fun execute(params: Params): TourScheduleResponse =
        repository.getTourScheduleAsync(
            productId = params.productId,
            date = params.date,
            to = params.to,
            currency = params.currency
        )
}
