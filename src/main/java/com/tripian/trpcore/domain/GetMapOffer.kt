package com.tripian.trpcore.domain

import com.mapbox.maps.CoordinateBounds
import com.tripian.one.api.pois.model.PoisResponse
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.domain.model.MapStep
import com.tripian.trpcore.repository.OfferRepository
import com.tripian.trpcore.repository.PoiRepository
import com.tripian.trpcore.util.extensions.poi2MapStep
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class GetMapOffer @Inject constructor(val repository: OfferRepository, val poiRepository: PoiRepository) :
    BaseUseCase<List<MapStep>, GetMapOffer.Params>() {

    class Params(val bounds: CoordinateBounds, val dateFrom: String, val dateTo: String)

    override fun on(params: Params?) {
        addObservable {
            val lat1: Double
            val lat2: Double
            val lon1: Double
            val lon2: Double

            val sw = params!!.bounds.southwest
            val ne = params.bounds.northeast

            if (sw.latitude() < ne.latitude()) {
                lat1 = sw.latitude()
                lat2 = ne.latitude()
            } else {
                lat1 = ne.latitude()
                lat2 = sw.latitude()
            }
            if (sw.longitude() < ne.longitude()) {
                lon1 = sw.longitude()
                lon2 = ne.longitude()
            } else {
                lon1 = ne.longitude()
                lon2 = sw.longitude()
            }

            val boundary = "${lat1},${lat2},${lon1},${lon2}"

//            repository.getOffersWithBoundary(dateFrom = params.dateFrom, dateTo = params.dateTo, boundary).flatMap { offerResponse ->
//                poiRepository.getPoiAlternatives(offerResponse.data!!.map { it.poiId }).map { poiResponse ->
//                    poi2Place(poiResponse)
//                }
//            }
            repository.getPoiOffersWithBoundary(dateFrom = params.dateFrom, dateTo = params.dateTo, boundary).map { poisResponse ->
                poi2Place(it = poisResponse)
            }
        }
    }

    private fun poi2Place(it: PoisResponse): List<MapStep> {
        val res = ArrayList<MapStep>()

        it.data?.forEach { poi ->
            res.add(poi2MapStep(poi, alternative = true).apply {
                this.order = -1
                this.isOffer = true
            })
        }

        return res.sortedBy { it.isOffer }
    }
}