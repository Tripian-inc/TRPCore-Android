package com.tripian.trpcore.domain

import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.base.TRPCore.Companion.placesApiKey
import com.tripian.trpcore.domain.model.PlaceAutocomplete
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class FetchPlace @Inject constructor() : BaseUseCase<Place?, FetchPlace.Params>() {

    private var placesClient: PlacesClient? = null

    class Params(val place: PlaceAutocomplete)

    override fun on(params: Params?) {
        val apiKey = placesApiKey

        // Log an error if apiKey is not set.
        if (apiKey.isEmpty() || apiKey == "DEFAULT_API_KEY") {
            return
        }
        if (!Places.isInitialized()) {
            Places.initialize(
                /* applicationContext = */ app.applicationContext,
                /* apiKey = */ apiKey
            )
        }

        if (placesClient == null) {
            placesClient = Places.createClient(app.applicationContext)
        }

        addObservable {
            PublishSubject.create {
                val request = FetchPlaceRequest.builder(
                    params!!.place.placeId.toString(),
                    listOf(Place.Field.ID, Place.Field.DISPLAY_NAME, Place.Field.LOCATION, Place.Field.FORMATTED_ADDRESS)
                ).build()

                placesClient!!.fetchPlace(request)
                    .addOnSuccessListener { res ->
                        it.onNext(res.place)
                    }.addOnFailureListener { err ->
                        it.onError(err)
                    }
            }
        }
    }

//    private fun getLocationRestriction(city: City): LocationRestriction? {
//        val southWest = city.boundary?[0].toString() + "," + city.boundary[2]
//        val northEast = city.boundary[1].toString() + "," + city.boundary[3]
//        return getBounds(southWest, northEast)
//    }
//
//    private fun getBounds(southWest: String, northEast: String): RectangularBounds? {
//        val bounds: LatLngBounds = convertToLatLngBounds(southWest, northEast) ?: return null
//        return RectangularBounds.newInstance(bounds)
//    }
}