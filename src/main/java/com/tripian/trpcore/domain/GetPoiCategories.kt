package com.tripian.trpcore.domain

import com.tripian.one.api.pois.model.PoiCategoryModel
import com.tripian.trpcore.base.SuspendUseCase
import com.tripian.trpcore.repository.PoiRepository
import javax.inject.Inject

class GetPoiCategories @Inject constructor(
    val poiRepository: PoiRepository
) : SuspendUseCase<PoiCategoryModel?, Unit>() {

    override suspend fun execute(params: Unit): PoiCategoryModel? =
        poiRepository.getPoiCategoriesAsync().data
}
