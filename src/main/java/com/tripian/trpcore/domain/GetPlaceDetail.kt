package com.tripian.trpcore.domain

import com.tripian.one.api.favorites.model.FavoriteResponse
import com.tripian.one.api.pois.model.Coordinate
import com.tripian.one.api.pois.model.PoiResponse
import com.tripian.one.api.trip.model.Step
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.domain.model.MapStep
import com.tripian.trpcore.domain.model.PlaceDetail
import com.tripian.trpcore.repository.FavoriteRepository
import com.tripian.trpcore.repository.OfferRepository
import com.tripian.trpcore.repository.PoiRepository
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.trpcore.ui.trip_detail.Mode
import com.tripian.trpcore.util.extensions.enableRating
import com.tripian.trpcore.util.extensions.formatDateDayShortName
import com.tripian.trpcore.util.extensions.getDays
import com.tripian.trpcore.util.extensions.mapIcons
import io.reactivex.Observable
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class GetPlaceDetail @Inject constructor(
    val tripModelRepository: TripModelRepository,
    val poiRepository: PoiRepository,
    val favoriteRepository: FavoriteRepository,
    val offerRepository: OfferRepository
) :
    BaseUseCase<PlaceDetail, GetPlaceDetail.Params>() {

    class Params(val poiId: String)

    override fun on(params: Params?) {
        addObservable {
            val trip = tripModelRepository.trip!!

            var foundedStep: Step? = null
            var inDay = 0
            val cityId = trip.city!!.id
            val poiId = params!!.poiId

            run loop@{
                trip.plans?.forEach { plan ->
                    plan.steps?.forEach { step ->
                        if (poiId == step.poi?.id) {
                            inDay++

                            return@loop
                        }
                    }

                    inDay++
                }
            }

            tripModelRepository.dailyPlan?.steps?.forEach {
                if (poiId == it.poi?.id) {
                    foundedStep = it

                    return@forEach
                }
            }

            if (foundedStep == null) {
                Observable.zip(
                    poiRepository.getPoiInfo(poiId),
                    favoriteRepository.getFavorite(cityId, params.poiId)
                ) { t1, t2 ->
                    poi2Place(t1, t2).apply {
                        mode = Mode.ADD
                    }
                }
            } else {
                Observable.zip(
                    Observable.just(PoiResponse().apply {
                        data = foundedStep.poi
                    }),
                    favoriteRepository.getFavorite(cityId, poiId)
                ) { t1, t2 ->
                    poi2Place(t1, t2).apply {
                        partOfDay = inDay
                        match = foundedStep.score
                        mode = Mode.REMOVE
                    }
                }
            }
        }
    }

    private fun poi2Place(poiInfo: PoiResponse, favInfo: FavoriteResponse?): PlaceDetail {
        val poi = poiInfo.data!!

        return PlaceDetail().apply {
            id = poi.id
            title = poi.name
            images = poi.gallery

            if (poi.enableRating()) {
                rating = poi.rating
                ratingCount = poi.ratingCount ?: 0
            } else {
                ratingCount = -1
                rating = -1f
            }

            cuisines = poi.cuisines
            tags = poi.tags?.joinToString(separator = ", ")
            hours =
                poi.hours?.getDays(tripModelRepository.dailyPlan?.date?.formatDateDayShortName())
            phone = poi.phone
            webSite = poi.web
            address = poi.address
            description = poi.description
            attention = poi.attention
            favorite = favInfo?.data

            offers = poi.offers

            if (!poi.mustTries.isNullOrEmpty()) {
                mustTry = "This spot serves one of the best ${
                    poi.mustTries?.map { it.name }?.joinToString(separator = ", ")
                } in ${tripModelRepository.trip?.city?.name}"
            }

            price = if (poi.price != null) {
                poi.price!!
            } else {
                -1
            }

            mapStep = MapStep().apply {
                group = "step"
                poiId = poi.id
                this.poi = poi
                name = poi.name
                description = poi.description
                image = poi.image?.url
                markerIcon = mapIcons[poi.icon] ?: -1

                order = order++
                coordinate = Coordinate().apply {
                    lat = poi.coordinate!!.lat
                    lng = poi.coordinate!!.lng
                }

                category = if (poi.category != null && poi.category!!.isNotEmpty()) {
                    poi.category!![0].name
                } else {
                    ""
                }

                if (poi.enableRating()) {
                    price = if (poi.price != null) {
                        poi.price!!
                    } else {
                        -1
                    }
                    ratingCount = poi.ratingCount ?: 0
                    rating = poi.rating
                } else {
                    ratingCount = -1
                    rating = -1f
                    price = -1
                }
            }
        }
    }
}