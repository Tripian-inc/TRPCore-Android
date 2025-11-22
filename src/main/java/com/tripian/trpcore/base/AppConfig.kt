package com.tripian.trpcore.base

/**
 * Created by Semih Özköroğlu on 29.07.2018.
 */
abstract class AppConfig {

    // 2 dk
    val SESSION_TIMEOUT: Long = 120

    var appLanguage: String = "en"

    /**
     * Service urls
     */
    abstract fun tripianServiceUrl(): String

    abstract fun apiKey(): String

    abstract fun mapboxApiKey(): String
}