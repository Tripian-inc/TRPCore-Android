package com.tripian.trpcore.domain

import android.text.TextUtils
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.domain.model.CitySelect
import com.tripian.trpcore.repository.TripRepository
import com.tripian.trpcore.util.extensions.clearTurkishChars
import java.util.Locale
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class SearchCity @Inject constructor(val repository: TripRepository) : BaseUseCase<List<CitySelect>, SearchCity.Params>() {

    class Params(val search: String)

    override fun on(params: Params?) {
        addObservable {
            repository.getCities(search = null, limit = 1000, page = null).map { res ->
                clearTurkishChars(params?.search)?.lowercase(Locale.getDefault())?.let { sText ->
                    ArrayList<CitySelect>().apply {

                        val continents = res.data?.map { it.country?.continent }?.distinctBy { it?.slug }

                        continents?.forEach { continent ->
                            res.data?.filter { TextUtils.equals(it.country?.continent?.slug, continent!!.slug) }?.let {
                                val filtered = it.filter { item ->
                                    clearTurkishChars(item.name)?.lowercase()?.contains(sText, ignoreCase = true)!! ||
                                            clearTurkishChars(item.country?.name)?.lowercase()?.contains(sText, ignoreCase = true)!!
                                }
                                if (filtered.isNotEmpty()) {
                                    val citySelectList = ArrayList<CitySelect>()
                                    citySelectList.add(CitySelect().apply {
                                        title = continent!!.name
                                        imageId = repository.getContinentImage(continent.slug ?: "")
                                    })
                                    citySelectList.addAll(filtered.map {
                                        CitySelect().apply {
                                            city = it
                                        }
                                    })
                                    addAll(citySelectList)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}