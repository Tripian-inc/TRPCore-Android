package com.tripian.trpcore.repository

import com.tripian.one.api.favorites.model.Favorite
import com.tripian.one.api.favorites.model.FavoriteRequest
import com.tripian.one.api.favorites.model.FavoriteResponse
import com.tripian.one.api.favorites.model.FavoritesResponse
import com.tripian.one.api.trip.model.DeleteResponse
import com.tripian.trpcore.util.extensions.internetConnectionAvailable
import com.tripian.trpcore.util.extensions.remove
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class FavoriteRepository @Inject constructor(val service: Service) {

    private var favorites = HashMap<Int, ArrayList<Favorite>>()
    private var favoriteEmitter = PublishSubject.create<Favorite>()

    fun addUserFavorite(cityId: Int, tripHash: String, poiId: String): Observable<FavoriteResponse> {
        return service.addUserFavorites(FavoriteRequest().apply {
            this.tripHash = tripHash
            this.poiId = poiId
        }).map {
            var cityFavorites = favorites[cityId]

            if (cityFavorites == null) {
                cityFavorites = ArrayList()

                favorites[cityId] = cityFavorites
            }

            if (!cityFavorites.any { f -> f.id == it.data?.id }) {
                it.data?.let {
                    it.isFavorite = true

                    cityFavorites.add(it)
                    favoriteEmitter.onNext(it)
                }
            }

            it
        }
    }

    fun deleteUserFavorite(cityId: Int, favoriteId: Int): Observable<DeleteResponse> {
        return service.deleteUserFavorites(favoriteId).map {
            var cityFavorites = favorites[cityId]

            if (cityFavorites == null) {
                cityFavorites = ArrayList()
            }

            it.data?.let {
                val item = cityFavorites.find { f -> f.id == favoriteId }
                item?.isFavorite = false

                cityFavorites.remove { f -> f.id == favoriteId }

                item?.let { favoriteEmitter.onNext(item) }
            }

            it
        }
    }

    fun getUserFavorites(cityId: Int): Observable<FavoritesResponse> {
        return if (!favorites[cityId].isNullOrEmpty()) {
            // TODO: How should the cache mechanism work?
            Observable.just(FavoritesResponse().apply {
                data = favorites[cityId]
                status = 200
            })
        } else {
            if (internetConnectionAvailable(3000)) {
                service.getUserFavorites(cityId, null, null).map {
                    // TODO: What should the limit be?
                    it.data?.let { favorites[cityId] = ArrayList(it) }

                    it
                }
            } else {
                if (favorites[cityId].isNullOrEmpty()) {
                    favorites[cityId] = ArrayList()
                }

                Observable.just(FavoritesResponse().apply {
                    data = favorites[cityId]
                    status = 200
                })
            }
        }
    }

    fun getFavorite(cityId: Int, poiId: String): Observable<FavoriteResponse> {
        return getUserFavorites(cityId).map {
            return@map FavoriteResponse().apply {
                data = it.data?.find { it.poiId == poiId }
                data?.isFavorite = true // Eger data null degilse favoridir

                status = 200
            }
        }
    }

    fun getFavoriteEmitter(): Observable<Favorite> {
        return favoriteEmitter
    }

    fun clearItems() {
        favorites.clear()

    }
}