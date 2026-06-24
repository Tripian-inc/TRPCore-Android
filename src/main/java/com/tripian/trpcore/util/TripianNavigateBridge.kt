package com.tripian.trpcore.util

import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.sdk.TRPCoreSDKListener

/**
 * Parameters forwarded to the host app when the SDK asks it to navigate to a
 * host-owned screen (e.g. a Capacitor webview route for a booked product).
 */
data class TripianNavigateParams(
    val type: String,
    val locator: String? = null,
    val detailURL: String? = null,
    val productId: String? = null,
    val date: String? = null
)

/**
 * Bridge between the SDK and a host app (e.g. the Nexus Capacitor plugin).
 *
 * The host assigns [onNavigate]; the bridge then installs a single
 * [TRPCoreSDKListener] on [TRPCore] and translates the SDK's already-wired
 * tap / booking / reservation callbacks into [TripianNavigateParams]. Assigning
 * `null` removes the listener.
 *
 * [sdkTaskId] / [sdkActivityClass] let the host bring the running SDK task back
 * to the foreground (populated by ACTimeline).
 *
 * Note: this installs itself as TRPCore's single SDK listener. A host that also
 * wants the raw [TRPCoreSDKListener] callbacks should use the bridge rather than
 * calling TRPCore.setListener directly.
 */
object TripianNavigateBridge {

    // These string values are the CONTRACT with the host web layer
    // (useTripianActivity payload.type). Keep them in sync with the web handlers:
    //   "product"      → MainLayout → /product-detail/:productId
    //   "availability" → MainLayout → /availability?product=…&date=…
    //   "booking"      → MainLayout → My Trips, opened by Locator (modal / detail URL)
    const val TYPE_ACTIVITY_DETAIL = "product"
    const val TYPE_BOOKING_DETAIL = "booking"
    const val TYPE_ACTIVITY_RESERVATION = "availability"

    /** Task id of the running SDK activity, for returnToSdk-style host actions. */
    var sdkTaskId: Int = -1

    /** The running SDK entry activity class, used as a returnToSdk fallback. */
    var sdkActivityClass: Class<*>? = null

    private val listener = object : TRPCoreSDKListener {
        override fun onRequestActivityDetail(activityId: String) {
            onNavigate?.invoke(
                TripianNavigateParams(type = TYPE_ACTIVITY_DETAIL, productId = activityId)
            )
        }

        override fun onRequestBookingDetail(bookingId: String) {
            // bookingId is the host-supplied booking reference (locator).
            onNavigate?.invoke(
                TripianNavigateParams(
                    type = TYPE_BOOKING_DETAIL,
                    locator = bookingId,
                    productId = bookingId
                )
            )
        }

        override fun onRequestActivityReservation(activityId: String, date: String?) {
            onNavigate?.invoke(
                TripianNavigateParams(
                    type = TYPE_ACTIVITY_RESERVATION,
                    productId = activityId,
                    date = date
                )
            )
        }

        override fun onTimelineCreated(tripHash: String) {
            // No navigation; a host needing tripHash can observe it elsewhere.
        }
    }

    /**
     * Host navigation callback. Assigning a non-null value installs the SDK
     * listener so taps inside the timeline drive [onNavigate]; assigning null
     * removes it.
     */
    var onNavigate: ((params: TripianNavigateParams) -> Unit)? = null
        set(value) {
            field = value
            TRPCore.setListener(if (value != null) listener else null)
        }
}
