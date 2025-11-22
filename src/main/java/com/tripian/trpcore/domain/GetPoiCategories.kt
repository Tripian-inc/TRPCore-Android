package com.tripian.trpcore.domain

import com.tripian.one.api.pois.model.PoiCategoryModel
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.PoiRepository
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class GetPoiCategories @Inject constructor(val poiRepository: PoiRepository) : BaseUseCase<PoiCategoryModel, Unit>() {

    override fun on(params: Unit?) {

        addObservable {
            poiRepository.getPoiCategories().map {
                it.data
            }
        }
    }
}