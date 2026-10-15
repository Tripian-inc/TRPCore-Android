package com.tripian.trpcore.ui.splash

import com.google.gson.annotations.SerializedName

/**
 * Nexus-specific reservation payload — the web app's `TripService` plus the
 * transformed fields it appends (full `detailURL`, lower-cased aliases).
 *
 * Consumed only by [ACSplashVM] to build an
 * [com.tripian.trpcore.domain.model.itinerary.ItineraryWithActivities]. Named
 * and kept standalone so the Nexus-specific structure is isolated from the rest
 * of the SDK. The full set of fields is mapped so additional ones can be used
 * later without touching the parser.
 */
internal class NexusReservationDto {
    @SerializedName("Date") val date: String? = null
    @SerializedName("DelegationId") val delegationId: Int? = null
    @SerializedName("Destination") val destination: String? = null
    @SerializedName("DetailUrl") val detailUrlRelative: String? = null
    @SerializedName("Direction") val direction: String? = null
    @SerializedName("FlightCode") val flightCode: String? = null
    @SerializedName("Id") val id: Long? = null
    @SerializedName("ProductId") val productId: Int? = null
    @SerializedName("BookingId") val bookingId: String? = null
    @SerializedName("DestinationId") val destinationId: Int? = null
    @SerializedName("IsScheduledGrouped") val isScheduledGrouped: Boolean? = null
    @SerializedName("Locator") val locator: String? = null
    @SerializedName("AgencyRef") val agencyRef: String? = null
    @SerializedName("MeetingPoint") val meetingPoint: String? = null
    @SerializedName("MeetingTimeAssigned") val meetingTimeAssigned: Boolean? = null
    @SerializedName("Origin") val origin: String? = null
    @SerializedName("PaxAdults") val paxAdults: Int? = null
    @SerializedName("PaxChildren") val paxChildren: Int? = null
    @SerializedName("PaxBabies") val paxBabies: Int? = null
    @SerializedName("Paxes") val paxes: List<String>? = null
    @SerializedName("QrValue") val qrValue: String? = null
    @SerializedName("Title") val title: String? = null
    @SerializedName("TrfType") val trfType: String? = null
    @SerializedName("Type") val type: String? = null
    @SerializedName("IsActiveTracking") val isActiveTracking: Boolean? = null
    @SerializedName("NexusBookingInformation") val nexusBookingInformation: NexusBookingInformationDto? = null

    // Transformed fields appended by the web app (front useTripianActivity).
    @SerializedName("detailURL") val detailURL: String? = null
    @SerializedName("meetingPoint") val meetingPointAlias: String? = null
    @SerializedName("locator") val locatorAlias: String? = null
    @SerializedName("productId") val productIdAlias: Int? = null
    @SerializedName("bookingId") val bookingIdAlias: String? = null
    @SerializedName("destinationId") val destinationIdAlias: Int? = null

    /** Catalogue product id as a non-empty string, or null when the host sent none. */
    val catalogueProductId: String?
        get() = (productId ?: productIdAlias)?.takeIf { it > 0 }?.toString()

    /** Juniper zone id as a non-empty string, or null when the host sent none. */
    val zoneId: String?
        get() = (destinationId ?: destinationIdAlias)?.takeIf { it > 0 }?.toString()

    /** Juniper line id of the booked service. */
    val juniperLineId: String?
        get() = (bookingId ?: bookingIdAlias)?.takeIf { it.isNotBlank() }

    // Optional pricing (present on some reservation types).
    @SerializedName("AmountClient") val amountClient: Double? = null
    @SerializedName("CurrencyClient") val currencyClient: String? = null
    @SerializedName("AmountOriginal") val amountOriginal: Double? = null
    @SerializedName("CurrencyOriginal") val currencyOriginal: String? = null

    internal class NexusBookingInformationDto {
        @SerializedName("CoverImageUrl") val coverImageUrl: String? = null
        @SerializedName("Description") val description: String? = null
        @SerializedName("CancellationPolicyDescription") val cancellationPolicyDescription: String? = null
    }
}
