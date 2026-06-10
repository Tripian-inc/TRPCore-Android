package com.tripian.trpcore.domain

import android.app.Application
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.tripian.trpcore.base.SuspendUseCase
import com.tripian.trpcore.base.TRPCore.Companion.placesApiKey
import com.tripian.trpcore.domain.model.PlaceAutocomplete
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FetchPlace @Inject constructor(
    private val app: Application
) : SuspendUseCase<Place?, FetchPlace.Params>() {

    private var placesClient: PlacesClient? = null

    class Params(val place: PlaceAutocomplete)

    override suspend fun execute(params: Params): Place? {
        val apiKey = placesApiKey
        if (apiKey.isEmpty() || apiKey == "DEFAULT_API_KEY") return null

        if (!Places.isInitialized()) {
            Places.initialize(app.applicationContext, apiKey)
        }
        if (placesClient == null) {
            placesClient = Places.createClient(app.applicationContext)
        }

        val request = FetchPlaceRequest.builder(
            params.place.placeId.toString(),
            listOf(
                Place.Field.ID,
                Place.Field.DISPLAY_NAME,
                Place.Field.LOCATION,
                Place.Field.FORMATTED_ADDRESS
            )
        ).build()

        return suspendCancellableCoroutine { cont ->
            placesClient!!.fetchPlace(request)
                .addOnSuccessListener { res ->
                    if (cont.isActive) cont.resume(res.place)
                }
                .addOnFailureListener { err ->
                    if (cont.isActive) cont.resumeWithException(err)
                }
        }
    }
}
