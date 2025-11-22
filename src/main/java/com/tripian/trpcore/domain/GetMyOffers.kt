package com.tripian.trpcore.domain

import com.tripian.one.api.pois.model.Poi
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.OfferRepository
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 23.04.2021.
 */
class GetMyOffers @Inject constructor(val offerRepository: OfferRepository) :
    BaseUseCase<ArrayList<Poi>, GetMyOffers.Params>() {

    class Params(val dateFrom: String?, val dateTo: String?, val isClear: Boolean? = true)

    override fun on(params: Params?) {
        addObservable {
            offerRepository.getMyOffers(
                dateFrom = params?.dateFrom,
                dateTo = params?.dateTo,
                isClear = params?.isClear ?: true
            )
        }
    }
}