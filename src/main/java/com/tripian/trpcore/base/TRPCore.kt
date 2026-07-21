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
import com.tripian.trpcore.di.DaggerAppComponent
import com.tripian.trpcore.domain.model.itinerary.ItineraryWithActivities
import com.tripian.trpcore.repository.MiscRepository
import com.tripian.trpcore.repository.TripRepository
import com.tripian.trpcore.sdk.TRPCoreErrorCode
import com.tripian.trpcore.sdk.TRPCoreSDKListener
import com.tripian.trpcore.ui.splash.ACSplash
import com.tripian.trpcore.ui.timeline.ACTimeline
import com.tripian.trpcore.util.CurrencyUtil
import com.tripian.trpcore.util.Preferences
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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

        const val EXTRA_ITINERARY = "extra_itinerary"
        const val EXTRA_TRIP_HASH = "extra_trip_hash"
        const val EXTRA_UNIQUE_ID = "extra_unique_id"
        const val EXTRA_CAN_BACK = "extra_can_back"
        const val EXTRA_APP_LANGUAGE = "extra_app_language"
        const val EXTRA_APP_CURRENCY = "extra_app_currency"

        /**
         * Active tour-api content provider — the single source of truth for all
         * tour-api operations. Defaults to [TripianProvider.CIVITATIS]; a host app
         * overrides it (e.g. Nexus sets [TripianProvider.NEXUS]). Read the numeric
         * id via `TRPCore.provider.id`.
         */
        @JvmStatic
        var provider: TripianProvider = TripianProvider.CIVITATIS

        /**
         * Per-host integration policy (Strategy pattern). The default is the
         * original SDK behavior (Civitatis); a host installs its own subclass
         * (e.g. [com.tripian.trpcore.base.host.NexusHostStrategy]) to customize
         * the SDK's extension points without adding host `if` branches anywhere.
         */
        @JvmStatic
        var host: com.tripian.trpcore.base.host.HostStrategy =
            com.tripian.trpcore.base.host.HostStrategy()

        private var listener: TRPCoreSDKListener? = null

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
         * Can be called from the host app to dismiss the SDK.
         */
        fun closeSDK() {
            activityStack.reversed().forEach { ref ->
                ref.get()?.finish()
            }
            activityStack.clear()

            listener?.onSDKDismissed()
        }

        /**
         * Starts the SDK from a host app with the user's reservations.
         *
         * The host passes the raw reservation payload; the SDK resolves the
         * destinations, performs the light login and builds/reuses the timeline
         * internally. The host does not need to know about the SDK's internal
         * entry Activity.
         *
         * @param reservations JSON array string (each item: TripService + detailURL)
         */
        @JvmStatic
        @JvmOverloads
        fun startWithReservations(
            context: Context,
            reservations: String,
            name: String? = null,
            lastName: String? = null,
            uniqueId: String? = null,
            language: String = "en",
            currency: String = "EUR"
        ) {
            val intent = Intent(context, ACSplash::class.java).apply {
                // Always start the SDK in its OWN task (ACSplash/ACTimeline carry a
                // distinct taskAffinity). This keeps the SDK alive in the background
                // when the host comes to the front for a detail screen, so the host
                // can bring it back exactly where the user left off (moveTaskToFront)
                // instead of the host's singleTask launch clearing it.
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("reservations", reservations)
                putExtra("name", name)
                putExtra("lastName", lastName)
                putExtra("uniqueId", uniqueId)
                putExtra("language", language)
                putExtra("currency", currency)
            }
            context.startActivity(intent)
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
            // Host policy: transform the raw activity id (tapped segment/step or
            // listing product) into the host detail-screen id. Idempotent.
            listener?.onRequestActivityDetail(host.activityDetailIdFromRaw(activityId))
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
            // Same host id transformation as activity detail (the reserve flow
            // opens the host availability/detail screen by product id).
            listener?.onRequestActivityReservation(host.activityDetailIdFromRaw(activityId), date)
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
        // STATIC CURRENCY WRAPPERS
        // =====================

        /**
         * Changes the app currency after SDK initialization.
         *
         * @param currency ISO 4217 currency code (EUR, USD, GBP, TRY, etc.)
         */
        fun changeCurrency(currency: String) {
            core.changeCurrency(currency)
        }

        /**
         * Gets the current currency code.
         *
         * @return Current ISO 4217 currency code
         */
        fun getCurrentCurrency(): String {
            return core.getCurrentCurrency()
        }

        /**
         * Gets the saved currency code from preferences.
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

        if (FirebaseApp.getApps(app).isEmpty()) {
            FirebaseApp.initializeApp(app)
        }

        fetchLanguages()

        return this
    }

    /**
     * Starts the Timeline screen directly with a trip hash.
     *
     * @param context The application context.
     * @param tripHash The timeline/trip hash to load.
     */
    fun startTimeline(
        context: Context,
        tripHash: String
    ) {
        val intent = ACTimeline.newIntent(context, tripHash)
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Primary SDK entry point. Fetches the existing timeline when a tripHash
     * is available, otherwise creates one from the itinerary model.
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
        require(itinerary.hasLocationData()) {
            "Either destinationItems or tripItems must contain at least one item with location data."
        }

        val effectiveUniqueId = uniqueId ?: itinerary.uniqueId
        val effectiveTripHash = tripHash ?: itinerary.tripianHash

        applyLanguageAndPrefetchCities(appLanguage)

        val intent = Intent(context, ACTimeline::class.java).apply {
            putExtra(EXTRA_ITINERARY, itinerary)
            putExtra(EXTRA_TRIP_HASH, effectiveTripHash)
            putExtra(EXTRA_UNIQUE_ID, effectiveUniqueId)
            putExtra(EXTRA_CAN_BACK, canBack)
            putExtra(EXTRA_APP_LANGUAGE, appLanguage)
            putExtra(EXTRA_APP_CURRENCY, appCurrency)
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
     * Fetches language values from the server during SDK initialization
     * on an IO dispatcher.
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
     * requested language.
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
     * Returns the TRPRest API client for direct API access.
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
     * resulting list to the host app on the main thread. Falls back to the
     * cached list if the request fails.
     */
    fun fetchCities(onComplete: (List<com.tripian.one.api.cities.model.City>) -> Unit) {
        initScope.launch {
            try {
                tripRepository.prefetchCitiesAsync()
            } catch (_: Throwable) {
            }
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                onComplete(tripRepository.getCachedCities())
            }
        }
    }
}