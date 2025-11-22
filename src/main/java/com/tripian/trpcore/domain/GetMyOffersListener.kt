package com.tripian.trpcore.domain

import com.tripian.one.api.offers.model.Offer
import com.tripian.one.api.pois.model.Poi
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.OfferRepository
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 23.04.2021.
 */
class GetMyOffersListener @Inject constructor(val offerRepository: OfferRepository) :
    BaseUseCase<ArrayList<Poi>, Unit>() {

    override fun on(params: Unit?) {
        addObservable {
            offerRepository.getMyOffersListener()
        }
    }

    override fun isRequiredRefreshToken(): Boolean {
        return false
    }
}