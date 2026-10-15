package com.tripian.trpcore.analytics

import com.tripian.trpcore.base.TRPCore

/**
 * Names of the analytics events the SDK emits through
 * [com.tripian.trpcore.sdk.TRPCoreSDKListener.onAnalyticsEvent].
 * Names are GA4-compatible (snake_case, max 40 chars) and prefixed with
 * `tripian_` so they never collide with host events.
 *
 * iOS equivalent: TRPAnalyticsEvent
 */
object TRPAnalyticsEvent {
    const val SDK_OPENED = "tripian_sdk_opened"
    const val SDK_CLOSED = "tripian_sdk_closed"
    const val SCREEN_VIEW = "tripian_screen_view"
    const val ERROR = "tripian_error"

    const val LOGIN_SUCCESS = "tripian_login_success"
    const val TIMELINE_CREATED = "tripian_timeline_created"
    const val TIMELINE_LOADED = "tripian_timeline_loaded"
    const val TIMELINE_DELETED = "tripian_timeline_deleted"
    const val TIMELINE_DAY_CHANGED = "tripian_timeline_day_changed"
    const val TIMELINE_RESET = "tripian_timeline_reset"
    const val TRIP_SELECTED = "tripian_trip_selected"

    const val CITY_SELECTED = "tripian_city_selected"
    const val DATES_SELECTED = "tripian_dates_selected"
    const val STARTING_POINT_SELECTED = "tripian_starting_point_selected"

    const val ADD_PLAN_OPENED = "tripian_add_plan_opened"
    const val SEARCH = "tripian_search"
    const val FILTER_APPLIED = "tripian_filter_applied"
    const val SORT_APPLIED = "tripian_sort_applied"

    const val STEP_ADDED = "tripian_step_added"
    const val STEP_REMOVED = "tripian_step_removed"
    const val STEP_TIME_CHANGED = "tripian_step_time_changed"
    const val STEP_REORDERED = "tripian_step_reordered"
    const val STEP_MOVED_TO_DAY = "tripian_step_moved_to_day"

    const val FAVORITE_ADDED = "tripian_favorite_added"
    const val FAVORITE_REMOVED = "tripian_favorite_removed"

    const val ACTIVITY_DETAIL_CLICK = "tripian_activity_detail_click"
    const val BOOKING_DETAIL_CLICK = "tripian_booking_detail_click"
    const val RESERVE_CLICK = "tripian_reserve_click"
    const val POI_DETAIL_VIEWED = "tripian_poi_detail_viewed"
    const val MAP_OPENED = "tripian_map_opened"
    const val DIRECTIONS_CLICK = "tripian_directions_click"
    const val WEB_PAGE_OPENED = "tripian_web_page_opened"
}

/**
 * Parameter keys shared by the SDK analytics events.
 *
 * iOS equivalent: TRPAnalyticsParam
 */
object TRPAnalyticsParam {
    const val SCREEN_NAME = "screen_name"
    const val ENTRY = "entry"
    const val SOURCE = "source"
    const val RESERVATION_COUNT = "reservation_count"
    const val TRIP_HASH = "trip_hash"
    const val CITY_ID = "city_id"
    const val CITY_NAME = "city_name"
    const val START_DATE = "start_date"
    const val END_DATE = "end_date"
    const val DAY_COUNT = "day_count"
    const val DAY_INDEX = "day_index"
    const val ITEM_ID = "item_id"
    const val ITEM_TYPE = "item_type"
    const val ITEM_NAME = "item_name"
    const val LISTING_TYPE = "listing_type"
    const val QUERY = "query"
    const val RESULT_COUNT = "result_count"
    const val CATEGORIES = "categories"
    const val SORT = "sort"
    const val TIME = "time"
    const val DATE = "date"
    const val ERROR_CODE = "error_code"
    const val ERROR_MESSAGE = "error_message"
    const val URL = "url"
}

/**
 * Screen identifiers reported with [TRPAnalyticsEvent.SCREEN_VIEW].
 *
 * iOS equivalent: TRPAnalyticsScreen
 */
object TRPAnalyticsScreen {
    const val SPLASH = "tripian_splash"
    const val MY_PLANS = "tripian_my_plans"
    const val CITY_SELECTION = "tripian_city_selection"
    const val DATE_SELECTION = "tripian_date_selection"
    const val STARTING_POINT_SELECTION = "tripian_starting_point_selection"
    const val TIMELINE = "tripian_timeline"
    const val ADD_PLAN = "tripian_add_plan"
    const val ACTIVITY_LISTING = "tripian_activity_listing"
    const val POI_LISTING = "tripian_poi_listing"
    const val POI_SELECTION = "tripian_poi_selection"
    const val POI_DETAIL = "tripian_poi_detail"
    const val SAVED_PLANS = "tripian_saved_plans"
    const val TIME_SELECTION = "tripian_time_selection"
    const val MAP = "tripian_map"
    const val WEB_PAGE = "tripian_web_page"
    const val ONBOARDING = "tripian_onboarding"
}

/**
 * Item types reported in [TRPAnalyticsParam.ITEM_TYPE].
 */
object TRPAnalyticsItemType {
    const val ACTIVITY = "activity"
    const val BOOKED_ACTIVITY = "booked_activity"
    const val POI = "poi"
    const val FAVORITE = "favorite"
    const val SMART_RECOMMENDATION = "smart_recommendation"
}

/**
 * Single emission point for SDK analytics. Forwards every event to the host
 * through the installed [com.tripian.trpcore.sdk.TRPCoreSDKListener]; the SDK
 * never talks to an analytics backend itself, so the host decides where events go.
 *
 * iOS equivalent: TRPAnalytics
 */
object TRPAnalytics {

    /**
     * Emits [event] with [params]; null values are dropped and string values are
     * truncated to the GA4 limit of 100 characters.
     */
    fun log(event: String, params: Map<String, Any?> = emptyMap()) {
        val listener = TRPCore.getListener() ?: return
        val sanitized = LinkedHashMap<String, Any>()
        params.forEach { (key, value) ->
            val cleaned = sanitize(value) ?: return@forEach
            sanitized[key] = cleaned
        }
        runCatching { listener.onAnalyticsEvent(event, sanitized) }
    }

    /**
     * Emits a [TRPAnalyticsEvent.SCREEN_VIEW] for [screen] with optional extra [params].
     */
    fun screen(screen: String, params: Map<String, Any?> = emptyMap()) {
        log(TRPAnalyticsEvent.SCREEN_VIEW, mapOf(TRPAnalyticsParam.SCREEN_NAME to screen) + params)
    }

    private fun sanitize(value: Any?): Any? = when (value) {
        null -> null
        is String -> value.take(MAX_STRING_LENGTH).ifEmpty { null }
        is Boolean -> if (value) 1 else 0
        is Number -> value
        is Enum<*> -> value.name.lowercase()
        is Collection<*> -> value.filterNotNull().joinToString(",").take(MAX_STRING_LENGTH).ifEmpty { null }
        else -> value.toString().take(MAX_STRING_LENGTH)
    }

    private const val MAX_STRING_LENGTH = 100
}
