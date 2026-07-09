package com.tripian.trpcore.di.modules

import android.app.Application
import android.os.Build
import android.util.Log
import com.tripian.one.TRPRest
import com.tripian.one.api.users.model.Device
import com.tripian.trpcore.util.Preferences
import com.tripian.trpcore.BuildConfig
import com.tripian.trpcore.base.AppConfig
import com.tripian.trpcore.repository.ServiceWrapper
import com.tripian.trpcore.util.extensions.getDeviceId
import dagger.Module
import dagger.Provides
import okhttp3.CacheControl
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Created by Semih Özköroğlu on 29.07.2018.
 */
@Module
class NetworkModule {

    /**
     * Mirrors the iOS SDK's User-Agent string:
     *   TRPCoreKit-Android (<bundleId>/<version>; Build/<build>; Android/<osVersion>; <device>)
     */
    private fun buildUserAgent(app: Application): String {
        val pkg = app.packageName
        val info = try {
            app.packageManager.getPackageInfo(pkg, 0)
        } catch (_: Exception) {
            null
        }
        val appVersion = info?.versionName ?: "0"
        val buildNumber = when {
            info == null -> "0"
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> info.longVersionCode.toString()
            else -> {
                @Suppress("DEPRECATION")
                info.versionCode.toString()
            }
        }
        val osVersion = Build.VERSION.RELEASE ?: "0"
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
        return "TRPCoreKit-Android ($pkg/$appVersion; Build/$buildNumber; Android/$osVersion; $deviceModel)"
    }

    @Provides
    @Singleton
    internal fun provideTOne(appConfig: AppConfig, app: Application, pref: Preferences): TRPRest {
        val tone = TRPRest(
            appContext = app,
            url = "${appConfig.tripianServiceUrl()}/${appConfig.apiVersion()}",
            key = appConfig.apiKey(),
            Device(
                deviceId = getDeviceId(pref),
                bundleId = "com.tripian.app",
                osVersion = Build.VERSION.SDK_INT.toString(),
                deviceOs = "android"
            )
        )

        return tone;
    }

    @Provides
    @Singleton
    internal fun provideServiceWrapper(app: Application, tone: TRPRest): ServiceWrapper {
        return ServiceWrapper(app, tone)
    }

    @Provides
    @Singleton
    internal fun provideCertificate(): CertificatePinner {
        return CertificatePinner.Builder()
            .build()
    }

    /** Sets User-Agent via `header(...)` so OkHttp's default is overwritten, not appended. */
    @Provides
    @Singleton
    internal fun provideOkHttp(appConfig: AppConfig, app: Application, pinner: CertificatePinner): OkHttpClient {
        val builder = OkHttpClient.Builder()
        val interceptor = HttpLoggingInterceptor {
            Log.e("OkHttp", it)
        }

        if (BuildConfig.DEBUG) {
            interceptor.level = HttpLoggingInterceptor.Level.BODY
        } else {
            interceptor.level = HttpLoggingInterceptor.Level.NONE
        }

        builder.certificatePinner(pinner)

        val userAgent = buildUserAgent(app)

        builder.addInterceptor { chain ->
            val originalRequest = chain.request()

            val newRequestBuilder = originalRequest.newBuilder()

            newRequestBuilder
                .header("User-Agent", userAgent)
                .addHeader("Content-Type", "application/json;charset=UTF-8")
                .cacheControl(CacheControl.FORCE_NETWORK)

            return@addInterceptor chain.proceed(newRequestBuilder.build())
        }

        builder.addInterceptor(interceptor)
        builder.connectTimeout(appConfig.SESSION_TIMEOUT, TimeUnit.SECONDS)
        builder.readTimeout(appConfig.SESSION_TIMEOUT, TimeUnit.SECONDS)
        builder.writeTimeout(appConfig.SESSION_TIMEOUT, TimeUnit.SECONDS)

        builder.hostnameVerifier { _, _ -> true }

        return builder.build()
    }
}