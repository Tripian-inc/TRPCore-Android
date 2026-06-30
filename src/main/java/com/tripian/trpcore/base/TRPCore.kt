package com.tripian.trpcore.base

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.FirebaseApp
import com.mapbox.common.MapboxOptions
import com.tripian.one.TRPRest
import com.tripian.trpcore.BuildConfig
import com.tripian.trpcore.di.DaggerAppComponent
import com.tripian.trpcore.domain.model.itinerary.ItineraryWithActivities
import com.tripian.trpcore.repository.MiscRepository
import com.tripian.trpcore.repository.TripRepository
import com.tripian.trpcore.sdk.TRPCoreErrorCode
import com.tripian.trpcore.sdk.TRPCoreSDKListener
import com.tripian.trpcore.ui.timeline.ACTimeline
import com.tripian.trpcore.util.CurrencyUtil
import com.tripian.trpcore.util.Preferences
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 23.07.2020.
 */
class TRPCore {

    companion object {
        lateinit var core: TRPCore
        lateinit var placesApiKey: String
        lateinit var mapBoxApiKey: String
        private const val BASE_URL = "https://gyssxjfp9d.execute-api.eu-west-1.amazonaws.com"
        private lateinit var apiVersion: String

        // Intent extra key'leri
        const val EXTRA_ITINERARY = "extra_itinerary"
        const val EXTRA_TRIP_HASH = "extra_trip_hash"
        const val EXTRA_UNIQUE_ID = "extra_unique_id"
        const val EXTRA_CAN_BACK = "extra_can_back"
        const val EXTRA_APP_LANGUAGE = "extra_app_language"
        const val EXTRA_APP_CURRENCY = "extra_app_currency"

        // SDK Listener - For host app callbacks
        private var listener: TRPCoreSDKListener? = null

        // Activity stack for tracking open SDK activities (WeakReference to avoid memory leaks)
        private val activityStack = mutableListOf<WeakReference<Activity>>()

        fun inject(activity: AppCompatActivity) {
            if (!::core.isInitialized) {
                throw IllegalStateException(
                    "TRPCore is not initialized. Call TRPCore().init() before using SDK activities."
                )
            }
            core.activityInjector().inject(activity)
        }

        // =====================
        // ACTIVITY TRACKING
        // =====================

        /**
         * Registers an activity to the SDK activity stack.
         * Called from BaseActivity.onCreate()
         */
        internal fun registerActivity(activity: Activity) {
            // Clean up any null references first
            activityStack.removeAll { it.get() == null }
            activityStack.add(WeakReference(activity))
        }

        /**
         * Unregisters an activity from the SDK activity stack.
         * Called from BaseActivity.onDestroy()
         */
        internal fun unregisterActivity(activity: Activity) {
            activityStack.removeAll { it.get() == activity || it.get() == null }
        }

        /**
         * Closes the SDK by finishing all open SDK activities.
         * This method can be called from the host app to dismiss the SDK.
         *
         * Usage:
         * ```
         * TRPCore.closeSDK()
         * ```
         */
        fun closeSDK() {
            // Finish all activities in reverse order (last opened first)
            activityStack.reversed().forEach { ref ->
                ref.get()?.finish()
            }
            activityStack.clear()

            // Notify host app that SDK is dismissed
            listener?.onSDKDismissed()
        }

        /**
         * Sets the SDK listener.
         * Through this listener, host app can receive SDK events.
         *
         * @param listener Callback interface implementation
         */
        fun setListener(listener: TRPCoreSDKListener?) {
            this.listener = listener
        }

        /**
         * Returns the current listener.
         */
        fun getListener(): TRPCoreSDKListener? = listener

        // =====================
        // CALLBACK HELPER METHODS
        // =====================

        /**
         * Triggers activity detail request callback
         */
        internal fun notifyActivityDetailRequested(activityId: String) {
            listener?.onRequestActivityDetail(activityId)
        }

        /**
         * Triggers booking detail request callback (booked_activity taps).
         */
        internal fun notifyBookingDetailRequested(bookingId: String) {
            listener?.onRequestBookingDetail(bookingId)
        }

        /**
         * Triggers activity reservation request callback
         *
         * @param activityId ID of the activity
         * @param date Date of the activity in "yyyy-MM-dd" format (null if not available)
         */
        internal fun notifyActivityReservationRequested(activityId: String, date: String? = null) {
            listener?.onRequestActivityReservation(activityId, date)
        }

        /**
         * Triggers the timeline created callback
         */
        internal fun notifyTimelineCreated(tripHash: String) {
            listener?.onTimelineCreated(tripHash)
        }

        /**
         * Triggers timeline loaded callback
         */
        internal fun notifyTimelineLoaded(tripHash: String) {
            listener?.onTimelineLoaded(tripHash)
        }

        /**
         * Triggers error callback (generic / uncategorized).
         */
        internal fun notifyError(error: String) {
            notifyError(error, TRPCoreErrorCode.GENERIC)
        }

        /**
         * Triggers error callback with a typed [code] so the host can switch on the category.
         */
        internal fun notifyError(error: String, code: TRPCoreErrorCode) {
            listener?.onError(error, code)
        }

        /**
         * Triggers SDK dismissed callback
         */
        internal fun notifySDKDismissed() {
            listener?.onSDKDismissed()
        }

        /**
         * Triggers activity added callback
         */
        internal fun notifyActivityAdded(activityId: String) {
            listener?.onActivityAdded(activityId)
        }

        /**
         * Triggers the "removed from saved plans" callback with the base activityId.
         */
        internal fun notifyActivityRemovedFromSavedPlans(activityId: String) {
            listener?.onActivityRemovedFromSavedPlans(activityId)
        }

        /**
         * Returns the device ID (fallback for uniqueId)
         */
        private fun getDeviceId(context: Context): String {
            return Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: "unknown_device"
        }

        // =====================
        // STATIC CURRENCY WRAPPERS (Backward Compatibility)
        // =====================

        /**
         * Changes the app currency after SDK initialization.
         * Static wrapper for backward compatibility.
         *
         * @param currency ISO 4217 currency code (EUR, USD, GBP, TRY, etc.)
         *
         * Usage:
         * ```kotlin
         * TRPCore.changeCurrency("USD")
         * ```
         */
        fun changeCurrency(currency: String) {
            core.changeCurrency(currency)
        }

        /**
         * Gets the current currency code.
         * Static wrapper for backward compatibility.
         *
         * @return Current ISO 4217 currency code
         */
        fun getCurrentCurrency(): String {
            return core.getCurrentCurrency()
        }

        /**
         * Gets the saved currency code from preferences.
         * Static wrapper for backward compatibility.
         *
         * @return Saved currency code or empty string if not set
         */
        fun getSavedCurrency(): String {
            return core.getSavedCurrency()
        }

        // =====================
        // ONBOARDING
        // =====================

        /**
         * Resets the onboarding state, allowing it to be shown again.
         * Call this if you want to show the onboarding to the user again.
         *
         * Usage:
         * ```kotlin
         * TRPCore.resetOnboarding(context)
         * ```
         *
         * @param context Application or Activity context
         */
        fun resetOnboarding(context: Context) {
            val prefs = Preferences(context)
            prefs.setBoolean(Preferences.Keys.ONBOARDING_HAS_SEEN, false)
            prefs.setInt(Preferences.Keys.ONBOARDING_CONTINUE_COUNT, 0)
            prefs.setBoolean(Preferences.Keys.ONBOARDING_DISMISSED_PERMANENTLY, false)
        }
    }

    @Inject
    lateinit var actInjector: DispatchingAndroidInjector<Activity>

    @Inject
    lateinit var miscRepository: MiscRepository

    @Inject
    lateinit var tripRepository: TripRepository

    @Inject
    lateinit var trpRest: TRPRest

    @Inject
    lateinit var appConfig: AppConfig

    // =====================
    // CURRENCY METHODS (Instance)
    // =====================

    /**
     * Changes the app currency after SDK initialization.
     * The currency is persisted and will be used for all subsequent API requests.
     *
     * @param currency ISO 4217 currency code (EUR, USD, GBP, TRY, JPY, AUD, CAD, CHF, MXN)
     *                 or locale format (es-MX, en-US, de-DE)
     *
     * Example usage:
     * ```kotlin
     * TRPCore.core.changeCurrency("USD")
     * TRPCore.core.changeCurrency("es-MX")  // Resolves to MXN
     * ```
     */
    fun changeCurrency(currency: String) {
        miscRepository.changeCurrency(currency)
    }

    /**
     * Gets the current currency code.
     * @return Current ISO 4217 currency code
     */
    fun getCurrentCurrency(): String {
        return miscRepository.getCurrentCurrency()
    }

    /**
     * Gets the saved currency code from preferences.
     * @return Saved currency code or empty string if not set
     */
    fun getSavedCurrency(): String {
        return miscRepository.getSavedCurrency()
    }

    /**
     * Returns the currency symbol for the given currency code.
     *
     * @param currencyCode The ISO 4217 currency code (e.g., "EUR", "USD")
     * @return The currency symbol, or the currency code itself if not found
     *
     * Example:
     * ```kotlin
     * TRPCore.core.getCurrencySymbol("EUR")  // Returns "€"
     * TRPCore.core.getCurrencySymbol("USD")  // Returns "$"
     * ```
     */
    fun getCurrencySymbol(currencyCode: String): String {
        return CurrencyUtil.getSymbol(currencyCode)
    }

    /**
     * Formats a price with the appropriate currency symbol.
     *
     * @param amount The price amount
     * @param currencyCode The currency code (uses current appCurrency if null)
     * @return Formatted price string (e.g., "€19.99")
     *
     * Example:
     * ```kotlin
     * TRPCore.core.formatPrice(19.99)        // Uses current currency
     * TRPCore.core.formatPrice(19.99, "USD") // Returns "$19.99"
     * ```
     */
    fun formatPrice(amount: Double, currencyCode: String? = null): String {
        return CurrencyUtil.formatPrice(amount, currencyCode)
    }

    /**
     * Formats a price with the appropriate currency symbol (Int version).
     *
     * @param amount The price amount
     * @param currencyCode The currency code (uses current appCurrency if null)
     * @return Formatted price string (e.g., "€19")
     */
    fun formatPrice(amount: Int, currencyCode: String? = null): String {
        return CurrencyUtil.formatPrice(amount, currencyCode)
    }

    /**
     * Returns a list of all supported currency codes.
     * @return List of ISO 4217 currency codes (EUR, USD, GBP, TRY, JPY, AUD, CAD, CHF, MXN)
     */
    fun getSupportedCurrencies(): List<String> {
        return CurrencyUtil.getSupportedCurrencies()
    }

    /**
     * Initializes the TRPCore SDK.
     *
     * @param app Application instance
     * @param tripianApiKey Tripian API key
     * @param placesApiKey Google Places API key
     * @param mapboxApiKey Mapbox API key
     * @param environment Environment (DEV, PREDEV, PROD) - determines API version path
     */
    fun init(
        app: Application,
        tripianApiKey: String,
        placesApiKey: String,
        mapboxApiKey: String,
        environment: Environment = Environment.PROD
    ): TRPCore {
        core = this
        Companion.placesApiKey = placesApiKey
        mapBoxApiKey = mapboxApiKey
        apiVersion = environment.getApiVersion()

        // Set Mapbox access token programmatically (instead of XML resource)
        if (Looper.myLooper() == Looper.getMainLooper()) {
            MapboxOptions.accessToken = mapboxApiKey
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        } else {
            Handler(Looper.getMainLooper()).post {
                MapboxOptions.accessToken = mapboxApiKey
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        DaggerAppComponent.builder()
            .configurations(object : AppConfig() {
                override fun tripianServiceUrl(): String {
                    return BASE_URL
                }

                override fun apiKey(): String {
                    return tripianApiKey
                }

                override fun mapboxApiKey(): String {
                    return mapboxApiKey
                }

                override fun apiVersion(): String {
                    return Companion.apiVersion
                }
            })
            .application(app)
            .build()
            .inject(this)

        // Conditional Firebase initialization - only if not already initialized by consumer app
        if (FirebaseApp.getApps(app).isEmpty()) {
            FirebaseApp.initializeApp(app)
        }

        // Fetch languages on SDK initialization
        fetchLanguages()

        return this
    }

    /**
     * Starts the Timeline screen directly with a trip hash.
     * This is useful for demo apps that want to show the timeline
     * without going through the full trip creation flow.
     *
     * @param context The application context.
     * @param tripHash The timeline/trip hash to load.
     */
    fun startTimeline(
        context: Context,
        tripHash: String
    ) {
        val intent = ACTimeline.newIntent(context, tripHash)
        // Only add FLAG_ACTIVITY_NEW_TASK for non-Activity context (backward compatible)
        // When called from Activity context, SDK runs in same task for proper back navigation
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * ⭐ MAIN ENTRY POINT - Starts the SDK with an itinerary model
     *
     * This method is the primary entry point for the SDK.
     * Creates a timeline from the itinerary model or fetches an existing one.
     *
     * Flow:
     * 1. Does tripHash exist? → Yes → Fetch timeline
     * 2. No tripHash? → Create timeline from itinerary
     *
     * @param context Android context
     * @param itinerary Itinerary data (destinations, activities, favorites)
     * @param tripHash Existing timeline hash if available (null to create new)
     * @param uniqueId User ID (device ID used if null)
     * @param canBack Whether to show back button
     * @param appLanguage App language (default: "en")
     * @param appCurrency App currency (default: "EUR")
     *
     * @throws IllegalArgumentException if neither destinationItems nor tripItems has data
     */
    fun startWithItinerary(
        context: Context,
        itinerary: ItineraryWithActivities,
        tripHash: String? = null,
        uniqueId: String? = null,
        canBack: Boolean = true,
        appLanguage: String = "en",
        appCurrency: String = "EUR"
    ) {
        // Validation - either destinationItems or tripItems must have data
        require(itinerary.hasLocationData()) {
            "Either destinationItems or tripItems must contain at least one item with location data."
        }

        val effectiveUniqueId = uniqueId ?: itinerary.uniqueId
        val effectiveTripHash = tripHash ?: itinerary.tripianHash

        applyLanguageAndPrefetchCities(appLanguage)

        // Fire-and-forget log - send itinerary parameters to backend
        sendItineraryLog(itinerary, tripHash, uniqueId, appLanguage, appCurrency)

        // Always launch ACTimeline directly. The activity owns the unified
        // "Getting your itinerary plan" loader covering both the language
        // retry and the initial timeline fetch — no host screen flicker, no
        // duplicated loaders. ACTimelineVM dispatches LANGUAGE_LOAD_FAILED
        // via the listener if translations can't be obtained.
        val intent = Intent(context, ACTimeline::class.java).apply {
            putExtra(EXTRA_ITINERARY, itinerary)
            putExtra(EXTRA_TRIP_HASH, effectiveTripHash)
            putExtra(EXTRA_UNIQUE_ID, effectiveUniqueId)
            putExtra(EXTRA_CAN_BACK, canBack)
            putExtra(EXTRA_APP_LANGUAGE, appLanguage)
            putExtra(EXTRA_APP_CURRENCY, appCurrency)
            // Only add FLAG_ACTIVITY_NEW_TASK for non-Activity context (backward compatible)
            // When called from Activity context, SDK runs in same task for proper back navigation
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(intent)
    }

    fun activityInjector(): AndroidInjector<Activity> {
        return actInjector
    }

    private val initScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Fetches language values from server. Called automatically during SDK
     * initialization on an IO dispatcher so the main thread stays free
     * (ANR prevention).
     */
    private fun fetchLanguages() {
        initScope.launch {
            try {
                val success = miscRepository.getLanguageValuesAsync()
                Log.d("TRPCore", "Languages fetched successfully: $success")
            } catch (error: Throwable) {
                Log.e("TRPCore", "Failed to fetch languages: ${error.message}")
            }
        }
    }


    /**
     * Applies the caller's [appLanguage] to AppConfig + TRPOne and then
     * background-refreshes the city cache so its translations match the
     * requested language. Invoked from every SDK start entry point — host apps
     * can re-open the SDK in a different language and get fresh city names
     * without an init-time mismatch.
     *
     * The [MiscRepository.changeLanguage] call re-selects the translation keys
     * before ACTimeline opens so its first loader text renders in the requested
     * language instead of flashing the previously selected one on the first open
     * after a language change.
     */
    private fun applyLanguageAndPrefetchCities(appLanguage: String) {
        if (appLanguage.isNotEmpty()) {
            appConfig.appLanguage = appLanguage
            trpRest.setLanguage(appLanguage)
            miscRepository.changeLanguage(appLanguage)
        }
        initScope.launch {
            try {
                tripRepository.prefetchCitiesAsync()
                Log.d(
                    "TRPCore",
                    "Cities pre-fetched successfully: ${tripRepository.getCachedCities().size} cities cached"
                )
            } catch (error: Throwable) {
                Log.e("TRPCore", "Failed to pre-fetch cities: ${error.message}")
            }
        }
    }

    /**
     * Sends itinerary parameters log to backend (fire-and-forget).
     * This is called when SDK is started with startWithItinerary.
     * The log is sent asynchronously and failures do not affect the user experience.
     * Only sends logs in release builds to avoid unnecessary API calls during development.
     */
    private fun sendItineraryLog(
        itinerary: ItineraryWithActivities,
        tripHash: String?,
        uniqueId: String?,
        appLanguage: String,
        appCurrency: String
    ) {
        // Only send logs in release builds
        if (BuildConfig.DEBUG) {
            return
        }

        try {
            val requestParams = mapOf<String, Any?>(
                "tripName" to (itinerary.tripName ?: ""),
                "startDatetime" to itinerary.startDatetime,
                "endDatetime" to itinerary.endDatetime,
                "uniqueId" to itinerary.uniqueId,
                "tripianHash" to (itinerary.tripianHash ?: ""),
                "destinationItems" to itinerary.destinationItems,
                "favouriteItems" to (itinerary.favouriteItems ?: emptyList<Any>()),
                "tripItems" to (itinerary.tripItems ?: emptyList<Any>()),
                "appLanguage" to appLanguage,
                "appCurrency" to appCurrency,
                "passedTripHash" to (tripHash ?: ""),
                "passedUniqueId" to (uniqueId ?: "")
            )

            val logMessage = mapOf(
                "platform" to "android",
                "type" to "INFO",
                "g_api_customer_id" to 0,
                "user_id" to 0,
                "endpoint" to "startWithItinerary",
                "response_msg" to "SDK started with itinerary",
                "request_params" to requestParams,
                "api_key" to appConfig.apiKey()
            )

            val logRequest = mapOf("message" to logMessage)
            val jsonBody = com.google.gson.Gson().toJson(logRequest)

            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val body = jsonBody.toRequestBody(mediaType)

            val ctxForUa = com.tripian.one.network.TConfig.appContext
            val pkg = ctxForUa.packageName
            val pInfo = try {
                ctxForUa.packageManager.getPackageInfo(pkg, 0)
            } catch (_: Exception) {
                null
            }
            val appVersion = pInfo?.versionName ?: "0"
            val buildNumber = when {
                pInfo == null -> "0"
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P -> pInfo.longVersionCode.toString()
                else -> {
                    @Suppress("DEPRECATION")
                    pInfo.versionCode.toString()
                }
            }
            val osVersion = android.os.Build.VERSION.RELEASE ?: "0"
            val deviceModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}".trim()
            val userAgent = "TRPCoreKit-Android ($pkg/$appVersion; Build/$buildNumber; Android/$osVersion; $deviceModel)"

            val request = okhttp3.Request.Builder()
                .url("${appConfig.tripianServiceUrl()}/${appConfig.apiVersion()}/misc/logs")
                .header("User-Agent", userAgent)
                .addHeader("x-api-key", appConfig.apiKey())
                .post(body)
                .build()

            // Fire-and-forget: Use enqueue for fully async call
            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    response.close()
                }
            })
        } catch (e: Exception) {
        }
    }

    /**
     * Returns the TRPRest API client for direct API access.
     * Useful for demo apps that need to create timelines or
     * make other API calls without going through the full SDK flow.
     *
     * @return TRPRest instance
     */
    fun getTRPRest(): TRPRest {
        return trpRest
    }

    /**
     * Get all cached cities.
     * Cities are pre-fetched at SDK initialization.
     *
     * @return List of cached City objects
     */
    fun getCachedCities(): List<com.tripian.one.api.cities.model.City> {
        return tripRepository.getCachedCities()
    }

    /**
     * Get a cached city by ID.
     * Cities are pre-fetched at SDK initialization.
     *
     * @param cityId City ID to look up
     * @return City if found, null otherwise
     */
    fun getCachedCityById(cityId: Int): com.tripian.one.api.cities.model.City? {
        return tripRepository.getCachedCityById(cityId)
    }

    /**
     * Refreshes the supported-cities cache from the API and delivers the
     * resulting list to the host app. Useful when the host has its own city
     * picker UI (e.g. a demo screen) and needs the full Tripian catalog
     * before the SDK is launched.
     *
     * - Hits the network; falls back to whatever is in the in-memory /
     *   SharedPreferences cache if the request fails so the callback always
     *   yields something to show.
     * - Idempotent — safe to call from multiple entry points.
     * - Callback is delivered on the main thread.
     */
    fun fetchCities(onComplete: (List<com.tripian.one.api.cities.model.City>) -> Unit) {
        initScope.launch {
            try {
                tripRepository.prefetchCitiesAsync()
            } catch (_: Throwable) {
                // Fall through to cache below.
            }
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                onComplete(tripRepository.getCachedCities())
            }
        }
    }
}