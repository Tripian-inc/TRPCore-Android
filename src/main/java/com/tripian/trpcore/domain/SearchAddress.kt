package com.tripian.trpcore.domain

import android.graphics.Typeface
import android.text.style.StyleSpan
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.LocationRestriction
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import com.google.android.libraries.places.api.net.PlacesClient
import com.tripian.one.api.cities.model.City
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.base.TRPCore.Companion.placesApiKey
import com.tripian.trpcore.domain.model.PlaceAutocomplete
import com.tripian.trpcore.util.extensions.convertToLatLngBounds
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject


/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class SearchAddress @Inject constructor() : BaseUseCase<List<PlaceAutocomplete>, SearchAddress.Params>() {

    private var placesClient: PlacesClient? = null

    class Params(val city: City, val search: String)

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
                val token = AutocompleteSessionToken.newInstance()

                val request = FindAutocompletePredictionsRequest.builder()
                    .setLocationRestriction(getLocationRestriction(params!!.city))
//                    .setCountries(listOf(params.city.country?.code))
                    .setSessionToken(token)
                    .setQuery(params.search)
                    .build()

                val prediction = placesClient!!.findAutocompletePredictions(request)

                placesClient!!.findAutocompletePredictions(request).addOnSuccessListener { response: FindAutocompletePredictionsResponse ->
                    val items = ArrayList<PlaceAutocomplete>()

                    if (prediction.isSuccessful && prediction.result != null) {
                        val normal = StyleSpan(Typeface.NORMAL)

                        for (p in prediction.result.autocompletePredictions) {
                            items.add(PlaceAutocomplete().apply {
                                placeId = p.placeId
                                area = p.getPrimaryText(normal).toString()
                                address = p.getSecondaryText(normal).toString()
                            })
                        }

                        it.onNext(items)
                    } else {
                        it.onNext(items)
                    }
                }.addOnFailureListener { ex ->
                    it.onError(ex)
                }
            }
        }
    }

    private fun getLocationRestriction(city: City): LocationRestriction? {
        val southWest = city.boundary?.get(0)?.toString() + "," + (city.boundary?.get(2)?.toString())
        val northEast = city.boundary?.get(1)?.toString() + "," + (city.boundary?.get(3)?.toString())
        return getBounds(southWest, northEast)
    }

    private fun getBounds(southWest: String, northEast: String): RectangularBounds? {
        val bounds: LatLngBounds = convertToLatLngBounds(southWest, northEast) ?: return null
        return RectangularBounds.newInstance(bounds)
    }
}