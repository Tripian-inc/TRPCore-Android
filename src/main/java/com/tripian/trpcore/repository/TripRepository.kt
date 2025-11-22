package com.tripian.trpcore.repository

import com.tripian.one.api.cities.model.City
import com.tripian.one.api.cities.model.GetCitiesResponse
import com.tripian.one.api.cities.model.GetCityResponse
import com.tripian.one.api.trip.model.DeleteResponse
import com.tripian.one.api.trip.model.TripRequest
import com.tripian.one.api.trip.model.TripResponse
import com.tripian.one.api.trip.model.TripsResponse
import com.tripian.trpcore.R
import io.reactivex.Observable
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class TripRepository @Inject constructor(val service: Service) {

    private var items = ArrayList<City>()

    fun getUserTrip(from: String?, to: String?, limit: Int, page: Int?): Observable<TripsResponse> {
        return service.getUserTrip(from, to, limit, page)
    }

    fun getCities(search: String?, limit: Int, page: Int?): Observable<GetCitiesResponse> {
        return if (items.isEmpty()) {
            service.getCities(search, limit, page).map { getCitiesResponse ->
                getCitiesResponse.data?.let { list ->
                    val sortedCities = list.sortedBy { it.name }
                    items.clear()
                    items.addAll(sortedCities)
                }

                GetCitiesResponse().apply {
                    data = items
                    status = 200
                }
            }
        } else {
            Observable.just(GetCitiesResponse().apply {
                data = items
                status = 200
            })
        }
    }

    fun getCity(cityId: Int): Observable<GetCityResponse> {
        return service.getCity(cityId).map {
            GetCityResponse().apply {
                data = it.data
                status = 200
            }
        }
    }

    fun fetchTrip(tripHash: String): Observable<TripResponse> {
        return service.fetchTrip(tripHash)
    }

    fun createTrip(request: TripRequest): Observable<TripResponse> {
        return service.createTrip(request)
    }

    fun updateTrip(tripHash: String, request: TripRequest): Observable<TripResponse> {
        return service.updateTrip(tripHash, request)
    }

    fun deleteTrip(tripHash: String): Observable<DeleteResponse> {
        return service.deleteTrip(tripHash)
    }

    fun clearItems() {
        items.clear()
    }

    fun getContinentImage(slug: String): Int {
        return when(slug) {
            "europe" -> R.drawable.im_europa
            "north-america" -> R.drawable.im_north_america
            "south-america" -> R.drawable.im_south_america
            "africa" -> R.drawable.im_africa
            "asia" -> R.drawable.im_asia
            "australia", "oceania" -> R.drawable.im_australia
            else -> {
                R.drawable.im_europa}
        }
    }
}