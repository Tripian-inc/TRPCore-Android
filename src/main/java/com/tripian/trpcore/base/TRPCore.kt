package com.tripian.trpcore.base

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.FirebaseApp
import com.tripian.gyg.base.Tripian
import android.provider.Settings
import com.tripian.trpcore.di.DaggerAppComponent
import com.tripian.trpcore.domain.model.itinerary.ItineraryWithActivities
import com.tripian.trpcore.repository.MiscRepository
import com.tripian.trpcore.repository.TripRepository
import com.tripian.trpcore.repository.authorization.AwsConfig
import com.tripian.trpcore.sdk.TRPCoreSDKListener
import com.tripian.trpcore.ui.splash.ACSplash
import com.tripian.trpcore.ui.timeline.ACTimeline
import com.tripian.one.TRPRest
import com.tripian.trpprovider.base.ProviderCore
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import com.mapbox.common.MapboxOptions
import io.reactivex.plugins.RxJavaPlugins
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 23.07.2020.
 */
class TRPCore {

    companion object {
        lateinit var core: TRPCore
        lateinit var placesApiKey: String
        lateinit var mapBoxApiKey: String
        lateinit var awsConfig: AwsConfig
        private const val BASE_URL = "https://gyssxjfp9d.execute-api.eu-west-1.amazonaws.com"
        private lateinit var apiVersion: String

        // Intent extra key'leri
        const val EXTRA_ITINERARY = "extra_itinerary"
        const val EXTRA_TRIP_HASH = "extra_trip_hash"
        const val EXTRA_UNIQUE_ID = "extra_unique_id"
        const val EXTRA_CAN_BACK = "extra_can_back"
        const val EXTRA_APP_LANGUAGE = "extra_app_language"

        // SDK Listener - For host app callbacks
        private var listener: TRPCoreSDKListener? = null

        fun inject(activity: AppCompatActivity) {
            core.activityInjector().inject(activity)
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
         * Triggers activity reservation request callback
         */
        internal fun notifyActivityReservationRequested(activityId: String) {
            listener?.onRequestActivityReservation(activityId)
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
         * Triggers error callback
         */
        internal fun notifyError(error: String) {
            listener?.onError(error)
        }

        /**
         * Triggers SDK dismissed callback
         */
        internal fun notifySDKDismissed() {
            listener?.onSDKDismissed()
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
    }

    @Inject
    lateinit var actInjector: DispatchingAndroidInjector<Activity>

    @Inject
    lateinit var miscRepository: MiscRepository

    @Inject
    lateinit var tripRepository: TripRepository

    @Inject
    lateinit var trpRest: TRPRest

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
        MapboxOptions.accessToken = mapboxApiKey

        ProviderCore().init(app, "")

        Tripian.setGetLanguage {
            miscRepository.getLanguageValueForKey(it)
        }

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        RxJavaPlugins.setErrorHandler {
//            FirebaseCrashlytics.getInstance().recordException(it)

            Log.e("AppError", it.stackTraceToString())
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

        // Pre-fetch cities on SDK initialization
        prefetchCities()

        return this
    }

    /**
     * Starts Tripian using the user's email and personal information.
     *
     * @param context The application context.
     * @param email The user's email address.
     * @param appLanguage The language code for translation (default "en").
     */
    fun startTripianWithEmail(
        context: Context,
        email: String,
        appLanguage: String = "en"
    ) {
        startTripianCore(
            context = context,
            email = email,
            uniqueId = null,
            appLanguage = appLanguage
        )
    }

    /**
     * Starts Tripian using a specific Unique ID.
     *
     * @param context The application context.
     * @param uniqueId The unique user identifier.
     * @param appLanguage The language code for translation (default "en").
     */
    fun startTripianWithUniqueId(
        context: Context,
        uniqueId: String,
        appLanguage: String = "en"
    ) {
        startTripianCore(
            context = context,
            email = null,
            uniqueId = uniqueId,
            appLanguage = appLanguage
        )
    }

    // Private core implementation to avoid code duplication
    private fun startTripianCore(
        context: Context,
        email: String? = null,
        uniqueId: String? = null,
        appLanguage: String
    ) {

        val intent = Intent(context, ACSplash::class.java)
        intent.putExtra("email", email)
        intent.putExtra("uniqueId", uniqueId)
        intent.putExtra("appLanguage", appLanguage)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
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
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
     *
     * @throws IllegalArgumentException if neither destinationItems nor tripItems has data
     */
    fun startWithItinerary(
        context: Context,
        itinerary: ItineraryWithActivities,
        tripHash: String? = null,
        uniqueId: String? = null,
        canBack: Boolean = true,
        appLanguage: String = "en"
    ) {
        // Validation - either destinationItems or tripItems must have data
        require(itinerary.hasLocationData()) {
            "Either destinationItems or tripItems must contain at least one item with location data."
        }

        val effectiveUniqueId = uniqueId ?: itinerary.uniqueId ?: getDeviceId(context)
        val effectiveTripHash = tripHash ?: itinerary.tripianHash

        // Ensure languages are loaded before opening timeline
        ensureLanguagesLoaded {
            val intent = Intent(context, ACTimeline::class.java).apply {
                putExtra(EXTRA_ITINERARY, itinerary)
                putExtra(EXTRA_TRIP_HASH, effectiveTripHash)
                putExtra(EXTRA_UNIQUE_ID, effectiveUniqueId)
                putExtra(EXTRA_CAN_BACK, canBack)
                putExtra(EXTRA_APP_LANGUAGE, appLanguage)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    /**
     * Opens an existing timeline with hash
     *
     * @param context Android context
     * @param tripHash Timeline hash
     * @param uniqueId User ID (device ID used if null)
     * @param canBack Whether to show back button
     * @param appLanguage App language (default: "en")
     */
    fun startWithTripHash(
        context: Context,
        tripHash: String,
        uniqueId: String? = null,
        canBack: Boolean = true,
        appLanguage: String = "en"
    ) {
        val effectiveUniqueId = uniqueId ?: getDeviceId(context)

        // Ensure languages are loaded before opening timeline
        ensureLanguagesLoaded {
            val intent = Intent(context, ACTimeline::class.java).apply {
                putExtra(EXTRA_TRIP_HASH, tripHash)
                putExtra(EXTRA_UNIQUE_ID, effectiveUniqueId)
                putExtra(EXTRA_CAN_BACK, canBack)
                putExtra(EXTRA_APP_LANGUAGE, appLanguage)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun activityInjector(): AndroidInjector<Activity> {
        return actInjector
    }

    /**
     * Fetches language values from server.
     * Called automatically during SDK initialization.
     */
    private fun fetchLanguages() {
        miscRepository.getLanguageValues()
            .subscribe(
                { success ->
                    Log.d("TRPCore", "Languages fetched successfully: $success")
                },
                { error ->
                    Log.e("TRPCore", "Failed to fetch languages: ${error.message}")
                }
            )
    }

    /**
     * Ensures languages are loaded before executing the action.
     * If already loaded, executes immediately.
     * If fetch is in progress (from init), waits for it to complete.
     * This prevents duplicate API calls and reduces user wait time.
     */
    private fun ensureLanguagesLoaded(onReady: () -> Unit) {
        if (miscRepository.isLanguagesLoaded) {
            onReady()
        } else {
            // Wait for ongoing fetch or start new one if needed
            miscRepository.waitForLanguagesLoaded()
                .subscribe(
                    { _ ->
                        onReady()
                    },
                    { error ->
                        Log.e("TRPCore", "Failed to fetch languages: ${error.message}")
                        // Still proceed even if fetch fails (will use cached or fallback)
                        onReady()
                    }
                )
        }
    }

    /**
     * Pre-fetches cities from server and caches them.
     * Called automatically during SDK initialization.
     * This ensures city data is available throughout the app for all city-related operations.
     */
    private fun prefetchCities() {
        tripRepository.prefetchCities()
            .subscribe(
                { success ->
                    Log.d("TRPCore", "Cities pre-fetched successfully: ${tripRepository.getCachedCities().size} cities cached")
                },
                { error ->
                    Log.e("TRPCore", "Failed to pre-fetch cities: ${error.message}")
                }
            )
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
}