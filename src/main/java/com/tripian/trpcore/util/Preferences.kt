package com.tripian.trpcore.util

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.core.content.edit
import com.tripian.trpcore.util.Preferences.Keys.PREFER_NAME
import javax.inject.Inject

class Preferences @Inject constructor(var context: Context) {

    object Keys {
        const val DEVICE_ID = "device_id"
        const val USER_LOGIN = "user_login"
        const val USER_LOGIN_TIME = "user_login_time"

        const val APP_LANGUAGE = "app_language"
        const val APP_LANGUAGE_TRANSLATIONS = "app_language_translations"
        const val APP_LANGUAGE_TRANSLATIONS_FETCHED_AT = "app_language_translations_fetched_at"

        /** Translation blobs are language scoped: the API returns a single language per response. */
        fun translationsForLanguage(lang: String) = "${APP_LANGUAGE_TRANSLATIONS}_$lang"

        fun translationsFetchedAtForLanguage(lang: String) =
            "${APP_LANGUAGE_TRANSLATIONS_FETCHED_AT}_$lang"
        const val APP_CURRENCY = "app_currency"
        const val CACHED_CITIES = "cached_cities"
        // TOKEN info
        const val TOKEN_TYPE = "TokenType"
        const val ACCESS_TOKEN = "AccessToken"
        const val REFRESH_TOKEN = "RefreshToken"
        const val SOCIAL_PROVIDER = "SocialProvider"

        // Onboarding
        const val ONBOARDING_HAS_SEEN = "trp_onboarding_has_seen"
        const val ONBOARDING_CONTINUE_COUNT = "trp_onboarding_continue_count"
        const val ONBOARDING_DISMISSED_PERMANENTLY = "trp_onboarding_dismissed_permanently"

        const val PREFER_NAME = "tone-preferences"
    }

    var pref: SharedPreferences = context.getSharedPreferences(PREFER_NAME, MODE_PRIVATE)

    fun getString(key: String, defValue: String): String {
        return pref.getString(key, defValue) ?: defValue
    }

    fun getString(key: String): String? {
        return pref.getString(key, "")
    }

    fun deleteKey(key: String) {
        pref.edit {
            remove(key)
        }
    }

    fun setString(key: String, newValue: String) {
        pref.edit {
            putString(key, newValue)
        }
    }

    fun setInt(key: String, newValue: Int) {
        pref.edit {
            putInt(key, newValue)
        }
    }

    fun setLong(key: String, newValue: Long) {
        pref.edit {
            putLong(key, newValue)
        }
    }

    fun setBoolean(key: String, newValue: Boolean?) {
        pref.edit {
            putBoolean(key, newValue!!)
        }
    }

    fun getInt(key: String, defValue: Int): Int {
        return pref.getInt(key, defValue)
    }

    fun getFloat(key: String, defValue: Float): Float {
        return pref.getFloat(key, defValue)
    }

    fun getLong(key: String, defValue: Long): Long {
        return pref.getLong(key, defValue)
    }

    fun getBoolean(key: String, defValue: Boolean): Boolean {
        return pref.getBoolean(key, defValue)
    }

    /**
     * Drops the signed-in session only. Everything the SDK can reuse across
     * sessions — device id, language and its translation cache, currency,
     * onboarding state, per-trip removed favorites — is left in place, so a
     * token failure doesn't reset the whole SDK for the user.
     *
     * TRPOne shares this preferences file, so its token keys are cleared here too.
     */
    fun clearSessionData() {
        pref.edit(commit = true) {
            listOf(
                Keys.USER_LOGIN,
                Keys.USER_LOGIN_TIME,
                Keys.TOKEN_TYPE,
                Keys.ACCESS_TOKEN,
                Keys.REFRESH_TOKEN,
                Keys.SOCIAL_PROVIDER
            ).forEach { remove(it) }
        }
    }

    fun clearAllData() {
        pref.edit(commit = true) {
            clear()
        }
    }
}