package com.tripian.trpcore.domain

import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.tripian.trpcore.base.SuspendUseCase
import com.tripian.trpcore.domain.model.PlaceAutocomplete
import com.tripian.trpcore.repository.PlacesClientProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FetchPlace @Inject constructor(
    private val placesClientProvider: PlacesClientProvider
) : SuspendUseCase<Place?, FetchPlace.Params>() {

    class Params(val place: PlaceAutocomplete)

    override suspend fun execute(params: Params): Place? {
        val client = placesClientProvider.getClient() ?: return null

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
            client.fetchPlace(request)
                .addOnSuccessListener { res ->
                    if (cont.isActive) cont.resume(res.place)
                }
                .addOnFailureListener { err ->
                    if (cont.isActive) cont.resumeWithException(err)
                }
        }
    }
}
