package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.one.api.tour.model.TourScheduleResponse
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.TourRepository
import javax.inject.Inject

/**
 * GetTourScheduleUseCase
 * Gets available time slots for a tour on a specific date
 */
class GetTourScheduleUseCase @Inject constructor(
    private val repository: TourRepository
) : BaseUseCase<TourScheduleResponse, GetTourScheduleUseCase.Params>() {

    data class Params(
        val productId: String,
        val date: String,           // Format: "YYYY-MM-DD"
        val currency: String? = "EUR"
    )

    override fun on(params: Params?) {
        params?.let { p ->
            addObservable {
                repository.getTourSchedule(
                    productId = p.productId,
                    date = p.date,
                    currency = p.currency
                ).toObservable()
            }
        }
    }
}
