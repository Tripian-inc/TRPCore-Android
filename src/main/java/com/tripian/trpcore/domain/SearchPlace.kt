package com.tripian.trpcore.domain

import android.text.TextUtils
import com.tripian.one.api.pois.model.PoisResponse
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.domain.model.PlaceItem
import com.tripian.trpcore.repository.PoiRepository
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.trpcore.repository.base.ErrorModel
import com.tripian.trpcore.repository.convertToPlaceItem
import com.tripian.trpcore.ui.trip.places.Place
import com.tripian.trpcore.util.Category
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class SearchPlace @Inject constructor(
    val tripModelRepository: TripModelRepository,
    val poiRepository: PoiRepository
) : BaseUseCase<List<PlaceItem>, SearchPlace.Params>() {

    private var nextLink: String? = ""
    var isLastPage = false
    var isLoading = false

    class Params(
        val cityId: Int?,
        val search: String?,
        val place: Place?,
        val categoryIds: List<Int>? = null
    )

    override fun on(params: Params?) {
        isLoading = true

        if (params!!.cityId == null && TextUtils.isEmpty(nextLink)) {
            onSendError(ErrorModel("City id cannot be blank. Please contact your administrator"))
        } else if (params.cityId == null && !TextUtils.isEmpty(nextLink)) {
            // Call with nextLink
//            addObservable { poiRepository.getPoiWithLink(nextLink!!).map(::poi2Place) }
        } else {
            if (params.categoryIds.isNullOrEmpty().not()) {
                addObservable {
                    poiRepository.search(params.cityId!!, params.search!!, params.categoryIds)
                        .map(::poi2Place)
                }
                return
            }
            val categories = when (params.place!!) {
                Place.ATTRACTIONS -> arrayListOf(
                    Category.ATTRACTIONS.id,
                    Category.RELIGIOUS_PLACE.id,
                    Category.MUSEUM.id,
                    Category.ART_GALLERY.id
                )

                Place.RESTAURANTS -> arrayListOf(Category.RESTAURANT.id)
                Place.CAFES -> arrayListOf(Category.CAFE.id, Category.BAKERY.id)
                Place.NIGHTLIFE -> arrayListOf(
                    Category.NIGHTLIFE.id,
                    Category.BAR.id,
                    Category.BREWERY.id
                )

                else -> arrayListOf(
                    Category.ATTRACTIONS.id,
                    Category.RELIGIOUS_PLACE.id,
                    Category.MUSEUM.id,
                    Category.ART_GALLERY.id
                )
            }

            addObservable {
                poiRepository.search(params.cityId!!, params.search!!, categories).map(::poi2Place)
            }
        }
    }

    private fun poi2Place(it: PoisResponse): List<PlaceItem> {
        isLastPage = TextUtils.isEmpty(nextLink)

        val res = ArrayList<PlaceItem>()

        it.data?.forEach { poi ->
            if (!res.any { it.id == poi.id }) {
                res.add(poi.convertToPlaceItem().apply {
                    stepId = tripModelRepository.getStepId(poi.id) ?: 0
                })
            }
        }

        isLoading = false

        return res
    }

    override fun onSendError(error: ErrorModel) {
        super.onSendError(error)

        isLoading = false
    }
}