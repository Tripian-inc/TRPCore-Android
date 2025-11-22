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
import com.tripian.trpcore.BuildConfig
import com.tripian.trpcore.di.DaggerAppComponent
import com.tripian.trpcore.repository.MiscRepository
import com.tripian.trpcore.repository.authorization.AwsConfig
import com.tripian.trpcore.ui.splash.ACSplash
import com.tripian.trpprovider.base.ProviderCore
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
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
        private var apiVersion: String = BuildConfig.BASE_API_VERSION // Default fallback

        fun inject(activity: AppCompatActivity) {
            core.activityInjector().inject(activity)
        }
    }

    @Inject
    lateinit var actInjector: DispatchingAndroidInjector<Activity>

    @Inject
    lateinit var miscRepository: MiscRepository

    fun init(
        app: Application,
        placesApi: String,
        tripianApiKey: String,
        mapboxApiKey: String,
        environment: Environment = Environment.PROD
    ): TRPCore {
        core = this
        placesApiKey = placesApi
        mapBoxApiKey = mapboxApiKey
        apiVersion = environment.getApiVersion()

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
                    return BuildConfig.BASE_TRIPIAN_URL
                }

                override fun apiKey(): String {
                    return tripianApiKey
                }

                override fun mapboxApiKey(): String {
                    return mapboxApiKey
                }

                override fun apiVersion(): String {
                    return apiVersion
                }
            })
            .application(app)
            .build()
            .inject(this)

        FirebaseApp.initializeApp(app)

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

    fun activityInjector(): AndroidInjector<Activity> {
        return actInjector
    }
}