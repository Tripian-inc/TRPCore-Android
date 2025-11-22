package com.tripian.trpcore.domain

import com.tripian.one.api.offers.model.OfferResponse
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.OfferRepository
import com.tripian.trpcore.util.OfferType
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 23.04.2021.
 */
class AddOffer @Inject constructor(val offerRepository: OfferRepository, val myOffers: GetMyOffers) : BaseUseCase<Unit, AddOffer.Params>() {

    class Params(val id: Int, val claimDate: String, val type: OfferType? = null)

    override fun on(params: Params?) {
        addObservable {
            offerRepository.addOffer(params!!.id, params.claimDate).flatMap {
                offerRepository.updateMyOffers()
            }
        }
    }

    override fun onSendSuccess(t: Unit) {
        myOffers.on()

        super.onSendSuccess(t)
    }
}