package com.tripian.trpcore.domain

import android.graphics.Typeface
import android.text.style.StyleSpan
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.LocationRestriction
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import com.tripian.one.api.cities.model.City
import com.tripian.trpcore.base.SuspendUseCase
import com.tripian.trpcore.domain.model.PlaceAutocomplete
import com.tripian.trpcore.repository.PlacesClientProvider
import com.tripian.trpcore.util.extensions.convertToLatLngBounds
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SearchAddress @Inject constructor(
    private val placesClientProvider: PlacesClientProvider
) : SuspendUseCase<List<PlaceAutocomplete>, SearchAddress.Params>() {

    class Params(val city: City, val search: String)

    override suspend fun execute(params: Params): List<PlaceAutocomplete> {
        val client = placesClientProvider.getClient() ?: return emptyList()

        val token = AutocompleteSessionToken.newInstance()
        val request = FindAutocompletePredictionsRequest.builder()
            .setLocationRestriction(getLocationRestriction(params.city))
            .setSessionToken(token)
            .setQuery(params.search)
            .build()

        return suspendCancellableCoroutine { cont ->
            client.findAutocompletePredictions(request)
                .addOnSuccessListener { response: FindAutocompletePredictionsResponse ->
                    val items = ArrayList<PlaceAutocomplete>()
                    val normal = StyleSpan(Typeface.NORMAL)
                    for (p in response.autocompletePredictions) {
                        items.add(PlaceAutocomplete().apply {
                            placeId = p.placeId
                            area = p.getPrimaryText(normal).toString()
                            address = p.getSecondaryText(normal).toString()
                        })
                    }
                    if (cont.isActive) cont.resume(items)
                }
                .addOnFailureListener { ex ->
                    if (cont.isActive) cont.resumeWithException(ex)
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
