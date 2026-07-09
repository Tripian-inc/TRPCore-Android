package com.tripian.trpcore.repository

import android.app.Application
import com.tripian.one.api.misc.model.ConfigList
import com.tripian.one.api.misc.model.ConfigListResponse
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.base.awaitCallback
import com.tripian.trpcore.util.CurrencyUtil
import com.tripian.trpcore.util.Preferences
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.closedText
import com.tripian.trpcore.util.extensions.fridayText
import com.tripian.trpcore.util.extensions.isConnectedNet
import com.tripian.trpcore.util.extensions.mondayText
import com.tripian.trpcore.util.extensions.saturdayText
import com.tripian.trpcore.util.extensions.sundayText
import com.tripian.trpcore.util.extensions.thursdayText
import com.tripian.trpcore.util.extensions.tuesdayText
import com.tripian.trpcore.util.extensions.wednesdayText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.ResponseBody
import org.json.JSONObject
import javax.inject.Inject

class MiscRepository @Inject constructor(
    var app: Application,
    val preferences: Preferences
) {
    private lateinit var languageValues: JSONObject
    private lateinit var currentLanguageValues: JSONObject
    var languageCodes: ArrayList<Pair<String, String>> = arrayListOf()

    private var configList: ConfigList? = null

    @Volatile
    var isLanguagesLoaded: Boolean = false
        private set

    /** Single Deferred coalesces concurrent fetches: subsequent callers await the same in-flight request. */
    private val fetchMutex = Mutex()
    private var inflight: Deferred<Boolean>? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Fetches language values from API.
     * Concurrent callers share the same in-flight request.
     */
    suspend fun getLanguageValuesAsync(): Boolean {
        if (isLanguagesLoaded) return true
        if (loadFreshCachedLanguages()) return true
        if (app.isConnectedNet().not()) return tryLoadCachedLanguages()
        return runOrJoinFetch()
    }

    /**
     * If already loaded, returns immediately. Otherwise piggybacks on (or
     * starts) the shared fetch.
     */
    suspend fun waitForLanguagesLoadedAsync(): Boolean {
        if (isLanguagesLoaded) return true
        return getLanguageValuesAsync()
    }

    /**
     * Forces a fresh /languages fetch, bypassing any cached in-flight result
     * (still respects the fresh-cache TTL).
     */
    suspend fun refetchLanguagesAsync(): Boolean {
        if (isLanguagesLoaded) return true
        if (loadFreshCachedLanguages()) return true
        if (app.isConnectedNet().not()) return tryLoadCachedLanguages()
        fetchMutex.withLock { inflight = null }
        return runOrJoinFetch()
    }

    private suspend fun runOrJoinFetch(): Boolean {
        val deferred = fetchMutex.withLock {
            inflight ?: scope.async { performFetch() }.also { inflight = it }
        }
        return try {
            deferred.await()
        } finally {
            fetchMutex.withLock {
                if (inflight === deferred) inflight = null
            }
        }
    }

    private suspend fun performFetch(): Boolean {
        return try {
            val body: ResponseBody = awaitCallback { ok, fail ->
                TRPCore.core.trpRest.getLanguageValues(success = ok, error = fail)
            }
            setLanguages(body.string())
            if (isLanguagesLoaded) {
                preferences.setLong(
                    Preferences.Keys.APP_LANGUAGE_TRANSLATIONS_FETCHED_AT,
                    System.currentTimeMillis()
                )
            }
            isLanguagesLoaded
        } catch (_: Throwable) {
            tryLoadCachedLanguages()
        }
    }

    /**
     * Loads the JSON blob persisted by the most recent successful `/languages`
     * response from preferences. Returns `true` only if [setLanguages] completes
     * without throwing AND flips [isLanguagesLoaded].
     */
    private fun tryLoadCachedLanguages(): Boolean {
        val cached = preferences.getString(Preferences.Keys.APP_LANGUAGE_TRANSLATIONS, "")
        if (cached.isNullOrEmpty()) return false
        return try {
            setLanguages(cached)
            isLanguagesLoaded
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Loads cached translations only when the persisted blob is younger than
     * [LANGUAGE_CACHE_TTL_MS]. Returns `true` on a successful in-window hit.
     */
    private fun loadFreshCachedLanguages(): Boolean {
        val fetchedAt = preferences.getLong(
            Preferences.Keys.APP_LANGUAGE_TRANSLATIONS_FETCHED_AT,
            0L
        )
        if (fetchedAt <= 0L) return false
        val age = System.currentTimeMillis() - fetchedAt
        if (age !in 0 until LANGUAGE_CACHE_TTL_MS) return false
        return tryLoadCachedLanguages()
    }

    suspend fun getConfigListAsync(): ConfigList? {
        configList?.let { return it }
        val response: ConfigListResponse = awaitCallback { ok, fail ->
            TRPCore.core.trpRest.getConfigList(success = ok, error = fail)
        }
        configList = response.data
        return response.data
    }

    private fun setLanguages(jsonText: String) {
        if (jsonText.isEmpty()) return
        val json = JSONObject(jsonText)
        languageValues = json.getJSONObject("translations")
        preferences.setString(Preferences.Keys.APP_LANGUAGE_TRANSLATIONS, jsonText)
        val langCodesJson = json.getJSONArray("lang_codes")
        for (i in 0 until langCodesJson.length()) {
            val item = langCodesJson.getJSONObject(i)
            languageCodes.add(Pair(item.getString("value"), item.getString("label")))
        }
        setCurrentLanguageKeys()
        setDaysTexts()
        isLanguagesLoaded = true
    }

    private fun setCurrentLanguageKeys() {
        val currentLang = preferences.getString(Preferences.Keys.APP_LANGUAGE)
        val resolvedLang = resolveLanguageCode(currentLang)
        TRPCore.core.appConfig.appLanguage = resolvedLang
        currentLanguageValues = languageValues.getJSONObject(resolvedLang).getJSONObject("keys")
    }

    /**
     * Resolves a language code to one that exists in available translations.
     * Handles null/empty values, regional locales (e.g., "es-MX" → "es"),
     * and falls back to "en" if not found.
     */
    private fun resolveLanguageCode(langCode: String?): String {
        if (langCode.isNullOrEmpty()) return "en"
        if (languageValues.has(langCode)) return langCode
        val baseLang = langCode.split("-", "_").firstOrNull()?.lowercase()
        if (!baseLang.isNullOrEmpty() && languageValues.has(baseLang)) {
            return baseLang
        }
        return "en"
    }

    private fun setDaysTexts() {
        mondayText = getLanguageValueForKey(LanguageConst.MONDAY)
        tuesdayText = getLanguageValueForKey(LanguageConst.TUESDAY)
        wednesdayText = getLanguageValueForKey(LanguageConst.WEDNESDAY)
        thursdayText = getLanguageValueForKey(LanguageConst.THURSDAY)
        fridayText = getLanguageValueForKey(LanguageConst.FRIDAY)
        saturdayText = getLanguageValueForKey(LanguageConst.SATURDAY)
        sundayText = getLanguageValueForKey(LanguageConst.SUNDAY)
        closedText = getLanguageValueForKey(LanguageConst.CLOSED)
    }

    fun changeLanguage(lang: String) {
        preferences.setString(Preferences.Keys.APP_LANGUAGE, lang)
        if (isLanguagesLoaded) setCurrentLanguageKeys()
    }

    /**
     * Changes the app currency and persists it.
     * The new currency will be used for all subsequent API requests.
     * @param currency ISO 4217 currency code (EUR, USD, GBP, TRY, etc.) or locale format (es-MX, en-US)
     */
    fun changeCurrency(currency: String) {
        val resolvedCurrency = CurrencyUtil.resolveCurrencyCode(currency)
        preferences.setString(Preferences.Keys.APP_CURRENCY, resolvedCurrency)
        TRPCore.core.appConfig.appCurrency = resolvedCurrency
    }

    fun getCurrentCurrency(): String {
        return TRPCore.core.appConfig.appCurrency
    }

    fun getSavedCurrency(): String {
        return preferences.getString(Preferences.Keys.APP_CURRENCY, "") ?: ""
    }

    fun getLanguageValueForKey(key: String): String {
        if (key.isEmpty()) return ""
        return try {
            getNestedValue(currentLanguageValues, key)
        } catch (_: Exception) {
            key
        }
    }

    /**
     * Gets a value from a JSONObject using dot notation for nested keys.
     * Falls back to direct key lookup if nested navigation fails.
     */
    private fun getNestedValue(json: JSONObject, key: String): String {
        if (json.has(key)) return json.getString(key)
        val parts = key.split(".")
        if (parts.size == 1) return json.getString(key)
        var current: Any = json
        for (i in 0 until parts.size - 1) {
            current = (current as JSONObject).getJSONObject(parts[i])
        }
        return (current as JSONObject).getString(parts.last())
    }

    fun getLanguageValueForKeyWithText(key: String, texts: List<String>): String {
        if (key.isEmpty()) return ""
        if (texts.isEmpty()) return getLanguageValueForKey(key)
        return try {
            val translatedText = getNestedValue(currentLanguageValues, key).replace("%s", "%S")
            String.format(translatedText, *texts.toTypedArray())
        } catch (_: Exception) {
            key
        }
    }

    companion object {
        /** Cold starts within this window load translations from preferences and skip /languages. */
        private const val LANGUAGE_CACHE_TTL_MS: Long = 60L * 60L * 1000L
    }
}
