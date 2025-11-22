package com.tripian.trpcore.domain

import com.tripian.gyg.util.extensions.getOfferDates
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.OfferRepository
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 23.04.2021.
 */
class RemoveOffer @Inject constructor(
    val offerRepository: OfferRepository,
    val myOffers: GetMyOffers
) : BaseUseCase<Unit, RemoveOffer.Params>() {

    class Params(val id: Int)

    override fun on(params: Params?) {
        addObservable {
            offerRepository.removeOffer(params!!.id).flatMap {
                offerRepository.updateMyOffers()
            }
        }
    }
}