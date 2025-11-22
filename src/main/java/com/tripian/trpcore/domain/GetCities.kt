package com.tripian.trpcore.domain

import android.text.TextUtils
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.domain.model.CitySelect
import com.tripian.trpcore.repository.TripRepository
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class GetCities @Inject constructor(val repository: TripRepository) : BaseUseCase<Pair<Boolean, List<CitySelect>>, Unit>() {

    override fun on(params: Unit?) {
        addObservable {
            repository.getCities(search = null, limit = 1000, page = null).map { res ->
                val isLastPage = res.pagination?.totalPages == res.pagination?.currentPage

                Pair(isLastPage, ArrayList<CitySelect>().apply {

                    val continents = res.data?.map { it.country?.continent }?.distinctBy { it?.slug }

                    continents?.forEach { continent ->
                        res.data?.filter { TextUtils.equals(it.country?.continent?.slug, continent!!.slug) }?.let {
                            if (it.isNotEmpty()) {
                                val citySelectList = ArrayList<CitySelect>()
                                citySelectList.add(CitySelect().apply {
                                    title = continent!!.name
                                    imageId = repository.getContinentImage(continent.slug ?: "")
                                })
                                citySelectList.addAll(it.map {
                                    CitySelect().apply {
                                        city = it
                                    }
                                })
                                addAll(citySelectList)
                            }
                        }
                    }
                })
            }
        }
    }
}