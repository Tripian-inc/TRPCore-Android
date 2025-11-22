package com.tripian.trpcore.domain

import com.mapbox.maps.CoordinateBounds
import com.tripian.one.api.pois.model.PoisResponse
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.domain.model.MapStep
import com.tripian.trpcore.repository.PoiRepository
import com.tripian.trpcore.util.extensions.poi2MapStep
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class SearchThisArea @Inject constructor(val poiRepository: PoiRepository) : BaseUseCase<List<MapStep>, SearchThisArea.Params>() {

    class Params(val bounds: CoordinateBounds, val distance: Double, val categories: List<Int>?)

    override fun on(params: Params?) {
        addObservable {
//            val boundary = "${params!!.bounds.latNorth},${params.bounds.latSouth},${params.bounds.lonWest},${params.bounds.lonEast}"

            poiRepository.getPoiWithBounds(params!!.bounds, params.categories).map(::poi2Place)
        }
    }

    private fun poi2Place(it: PoisResponse): List<MapStep> {
        val res = ArrayList<MapStep>()

        it.data?.forEach { poi ->
            res.add(poi2MapStep(poi, alternative = true).apply {
                this.order = -1
            })
        }

        return res
    }
}