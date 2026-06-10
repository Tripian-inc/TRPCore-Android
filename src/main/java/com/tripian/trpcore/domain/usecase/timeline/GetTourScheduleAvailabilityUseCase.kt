package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.one.api.tour.model.TourScheduleAvailabilityResponse
import com.tripian.trpcore.base.SuspendUseCase
import com.tripian.trpcore.repository.TourRepository
import javax.inject.Inject

/**
 * Batch availability lookup across multiple activities on a single target date.
 *
 * Backed by `POST /tour-api/schedule-bulk`.
 *
 * Per-item interpretation in the response:
 *  - missing from `data.schedules`  → expired (no longer available on that date)
 *  - present with `schedule == null` → sold out / no slots that day
 *  - present with non-empty slots    → available
 */
class GetTourScheduleAvailabilityUseCase @Inject constructor(
    private val repository: TourRepository
) : SuspendUseCase<TourScheduleAvailabilityResponse,
        GetTourScheduleAvailabilityUseCase.Params>() {

    data class Params(
        val items: List<String>,    // ["C_xxx_15", ...]
        val date: String,           // "YYYY-MM-DD"
        val currency: String? = null,
        val lang: String? = null
    )

    override suspend fun execute(params: Params): TourScheduleAvailabilityResponse =
        repository.getTourScheduleAvailabilityAsync(
            items = params.items,
            date = params.date,
            currency = params.currency,
            lang = params.lang
        )
}
