package com.tripian.trpcore.sdk

/**
 * Categorizes errors surfaced through [TRPCoreSDKListener.onError].
 * Host apps can switch on this to provide user-facing messages or recovery actions.
 */
enum class TRPCoreErrorCode {
    /** Default — any uncategorized SDK error. */
    GENERIC,

    /** Frontend translations could not be fetched after retry; SDK was not opened. */
    LANGUAGE_LOAD_FAILED,

    /**
     * A booked activity supplied in the itinerary could not be written to the
     * timeline (e.g. the server rejected its date). The rest of the timeline is
     * usable; that one activity is missing from it.
     */
    BOOKED_ACTIVITY_SYNC_FAILED
}

/**
 * TRPCoreSDKListener - SDK callback interface
 *
 * Host app implements this interface to receive events from the SDK.
 * Through these callbacks:
 * - Activity detail screen can be opened
 * - Reservation flow can be started
 * - tripHash can be saved when timeline is created
 *
 * iOS equivalent: TRPCoreKitDelegate
 */
interface TRPCoreSDKListener {

    /**
     * Called when user taps on an activity card.
     * Host app should open activity detail screen in this callback.
     *
     * @param activityId ID of the tapped activity (e.g., "15423" or product ID)
     *
     * iOS equivalent: trpCoreKitDidRequestActivityDetail(activityId:)
     */
    fun onRequestActivityDetail(activityId: String)

    /**
     * Called when user taps on a booked_activity card.
     * Host app should open the booking detail screen in this callback.
     *
     * Default implementation delegates to [onRequestActivityDetail].
     * Override this when you need a dedicated booking detail flow.
     *
     * @param bookingId ID of the tapped booking (sourced from segment additionalData)
     *
     * iOS equivalent: trpCoreKitDidRequestBookingDetail(bookingId:)
     */
    fun onRequestBookingDetail(bookingId: String) {
        onRequestActivityDetail(bookingId)
    }

    /**
     * Called when user taps "Reserve" or "Book" button.
     * Host app should start reservation/booking flow in this callback.
     *
     * @param activityId ID of the activity to be reserved
     * @param date Date of the activity in "yyyy-MM-dd" format (e.g., "2026-04-17")
     *             Null if date is not available.
     *
     * iOS equivalent: trpCoreKitDidRequestActivityReservation(activityId:)
     */
    fun onRequestActivityReservation(activityId: String, date: String? = null)

    /**
     * Called when timeline is successfully created.
     * Host app should save tripHash in this callback and use it
     * to access the same timeline in the future.
     *
     * @param tripHash Unique hash of the created timeline
     *
     * iOS equivalent: trpCoreKitDidCreateTimeline(tripHash:)
     */
    fun onTimelineCreated(tripHash: String)

    /**
     * Called when timeline is loaded (after fetch or create).
     * Optional callback - default implementation is empty.
     *
     * @param tripHash Hash of the loaded timeline
     */
    fun onTimelineLoaded(tripHash: String) {}

    /**
     * Called when an error occurs in the SDK.
     * Optional callback - default implementation is empty.
     *
     * @param error Error message
     */
    fun onError(error: String) {}

    /**
     * Called when a categorized error occurs in the SDK.
     * Default implementation delegates to [onError].
     * Override this when you need to switch on [code] (e.g. show a
     * dedicated retry UI for [TRPCoreErrorCode.LANGUAGE_LOAD_FAILED]).
     *
     * @param error Error message
     * @param code Categorizes the error so the host can react accordingly
     */
    fun onError(error: String, code: TRPCoreErrorCode) {
        onError(error)
    }

    /**
     * Called when user exits the SDK (back pressed).
     * Optional callback - default implementation is empty.
     */
    fun onSDKDismissed() {}

    /**
     * Called when an activity is successfully added to the timeline.
     * Host app can use this to track manually added activities.
     *
     * @param activityId ID of the added activity (productId)
     */
    fun onActivityAdded(activityId: String) {}

    /**
     * Called when the user removes an activity from their saved plans (favorites)
     * via the Saved Plans flow. Host app should remove it from its own favorites
     * store so the two stay in sync.
     *
     * @param activityId Base activity ID with the "C_" prefix and provider/city
     *                   suffixes stripped (e.g. "15423").
     */
    fun onActivityRemovedFromSavedPlans(activityId: String) {}
}
