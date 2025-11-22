package com.tripian.trpcore.domain

import android.text.TextUtils
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.domain.model.PlaceItem
import com.tripian.trpcore.repository.base.ErrorModel
import com.tripian.trpcore.repository.PoiRepository
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.trpcore.util.extensions.enableRating
import com.tripian.one.api.pois.model.PoisResponse
import com.tripian.trpcore.repository.convertToPlaceItem
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class GetMustTries @Inject constructor(val poiRepository: PoiRepository, val tripModelRepository: TripModelRepository) : BaseUseCase<List<PlaceItem>, GetMustTries.Params>() {

    private var nextLink: String? = ""
    var isLastPage = false
    var isLoading = false

    class Params(val mustTryIds: Int?)

    override fun on(params: Params?) {
        isLoading = true

        if (params!!.mustTryIds == null && TextUtils.isEmpty(nextLink)) {
            onSendError(ErrorModel("City id cannot be blank. Please contact your administrator"))
        } else if (params.mustTryIds == null && !TextUtils.isEmpty(nextLink)) {
            // Call with nextLink
//            addObservable { poiRepository.getPoiWithLink(nextLink!!).map(::poi2Place) }
        } else {
            addObservable {
                poiRepository.getPoiWithTaste(tripModelRepository.trip!!.city!!.id, mustTryIds = params.mustTryIds!!).map(::poi2Place)
            }
        }
    }

    private fun poi2Place(it: PoisResponse): List<PlaceItem> {
        isLastPage = TextUtils.isEmpty(nextLink)

        val res = ArrayList<PlaceItem>()

        it.data?.forEach { poi ->
            if (!res.any { it.id == poi.id }) {
                res.add(poi.convertToPlaceItem())
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