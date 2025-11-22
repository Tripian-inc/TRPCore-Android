package com.tripian.trpcore.domain

import com.tripian.one.api.pois.model.PoisResponse
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.domain.model.PlaceItem
import com.tripian.trpcore.repository.PoiRepository
import com.tripian.trpcore.repository.base.ErrorModel
import com.tripian.trpcore.repository.convertToPlaceItem
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class GetPlaceWithCategory @Inject constructor(val poiRepository: PoiRepository) :
    BaseUseCase<List<PlaceItem>, GetPlaceWithCategory.Params>() {

    private var categories: List<Int>? = null
    var isLastPage = false
    var isLoading = false
    var page = 1

    class Params(val cityId: Int?, val categoryIds: List<Int>)

    override fun on(params: Params?) {
        isLoading = true

        categories = params!!.categoryIds

        addObservable {
            poiRepository.getPoiWithCategories(params.cityId!!, categories!!, page, 100)
                .map(::poi2Place)
        }
    }

    private fun poi2Place(it: PoisResponse): List<PlaceItem> {
        isLastPage = it.pagination?.currentPage == it.pagination?.totalPages

        val res = ArrayList<PlaceItem>()

        it.data?.forEach { poi ->
            val poiCategories = poi.category
            if (poiCategories.isNullOrEmpty().not()) {
                if (poiCategories.any { stepCategory ->
                        (categories?.find { c -> c == stepCategory.id } != null && categories?.find { c -> c == stepCategory.id } != -1)
                    }) {

                    if (!res.any { it.id == poi.id }) {
                        res.add(poi.convertToPlaceItem())
                    }
                }
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