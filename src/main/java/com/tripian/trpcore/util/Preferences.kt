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
        // TOKEN info
        const val TOKEN_TYPE = "TokenType"
        const val ACCESS_TOKEN = "AccessToken"
        const val REFRESH_TOKEN = "RefreshToken"
        const val SOCIAL_PROVIDER = "SocialProvider"

        // Shared preferences file name
        const val PREFER_NAME = "tone-preferences"
    }

    // default mode private
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

    fun clearAllData() {
        pref.edit(commit = true) {
            clear()
        }
    }
}