package com.tripian.trpcore.sdk

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
}
