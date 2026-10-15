package com.tripian.trpcore.repository

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.ResponseBody
import org.json.JSONObject

/**
 * @param now Clock used for cache freshness checks
 */
class MiscRepository(
    var app: Application,
    val preferences: Preferences,
    private val now: () -> Long = System::currentTimeMillis
) {
    @Volatile
    private var languageValues: JSONObject? = null

    @Volatile
    private var currentLanguageValues: JSONObject? = null

    @Volatile
    var languageCodes: List<Pair<String, String>> = emptyList()

    @Volatile
    private var configList: ConfigList? = null

    @Volatile
    var isLanguagesLoaded: Boolean = false
        private set

    @Volatile
    private var loadedLanguage: String? = null

    private val _appliedLanguage = MutableLiveData<String>()

    /** Emits every time the in-memory key table starts serving a different language. */
    val appliedLanguage: LiveData<String> = _appliedLanguage

    /** Single Deferred coalesces concurrent fetches: subsequent callers await the same in-flight request. */
    private val fetchMutex = Mutex()
    private var inflight: Deferred<Boolean>? = null
    private var inflightLanguage: String? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Fetches the translation blob for the active language.
     * Concurrent callers requesting the same language share one in-flight request.
     */
    suspend fun getLanguageValuesAsync(): Boolean {
        val language = activeLanguage()
        if (isLoadedFor(language)) return true
        if (loadFreshCachedLanguages(language)) return true
        if (app.isConnectedNet().not()) return tryLoadCachedLanguages(language)
        return runOrJoinFetch(language)
    }

    /**
     * If already loaded, returns immediately. Otherwise piggybacks on (or
     * starts) the shared fetch.
     */
    suspend fun waitForLanguagesLoadedAsync(): Boolean {
        if (isLoadedFor(activeLanguage())) return true
        return getLanguageValuesAsync()
    }

    /**
     * Forces a fresh translations fetch, bypassing any cached in-flight result
     * (still respects the fresh-cache TTL).
     */
    suspend fun refetchLanguagesAsync(): Boolean {
        val language = activeLanguage()
        if (isLoadedFor(language)) return true
        if (loadFreshCachedLanguages(language)) return true
        if (app.isConnectedNet().not()) return tryLoadCachedLanguages(language)
        fetchMutex.withLock {
            inflight = null
            inflightLanguage = null
        }
        return runOrJoinFetch(language)
    }

    private fun isLoadedFor(language: String): Boolean {
        return isLanguagesLoaded && loadedLanguage == language
    }

    /**
     * Language the SDK should render in: the persisted selection, falling back
     * to the configured one and finally to [DEFAULT_LANGUAGE].
     */
    private fun activeLanguage(): String {
        val saved = preferences.getString(Preferences.Keys.APP_LANGUAGE, "")
        if (saved.isNotBlank()) return normalizeLanguage(saved)
        val configured = runCatching { TRPCore.core.appConfig.appLanguage }.getOrNull()
        return configured?.takeIf { it.isNotBlank() }?.let { normalizeLanguage(it) }
            ?: DEFAULT_LANGUAGE
    }

    /** The API expects hyphenated lowercase codes ("pt-br"); locale forms like "pt_BR" are not resolved. */
    private fun normalizeLanguage(lang: String): String {
        return lang.trim().replace('_', '-').lowercase()
    }

    private suspend fun runOrJoinFetch(language: String): Boolean {
        val deferred = fetchMutex.withLock {
            val existing = inflight
            if (existing != null && inflightLanguage == language) {
                existing
            } else {
                scope.async { performFetch(language) }.also {
                    inflight = it
                    inflightLanguage = language
                }
            }
        }
        return try {
            deferred.await()
        } finally {
            fetchMutex.withLock {
                if (inflight === deferred) {
                    inflight = null
                    inflightLanguage = null
                }
            }
        }
    }

    /**
     * The endpoint resolves the language from the request query TRPOne appends,
     * so the config language is aligned with [language] before the call.
     */
    private suspend fun performFetch(language: String): Boolean {
        return try {
            TRPCore.core.appConfig.appLanguage = language
            val body: ResponseBody = awaitCallback { ok, fail ->
                TRPCore.core.trpRest.getLanguageValuesV2(success = ok, error = fail)
            }
            val jsonText = body.string()
            setLanguages(jsonText, language)
            if (isLoadedFor(language)) {
                cacheTranslations(language, jsonText)
            }
            isLoadedFor(language)
        } catch (_: Throwable) {
            tryLoadCachedLanguages(language)
        }
    }

    private fun cacheTranslations(language: String, jsonText: String) {
        preferences.setString(Preferences.Keys.translationsForLanguage(language), jsonText)
        preferences.setLong(
            Preferences.Keys.translationsFetchedAtForLanguage(language),
            System.currentTimeMillis()
        )
        dropLegacyTranslationCache()
    }

    /** Removes the single all-languages blob persisted by earlier SDK versions. */
    private fun dropLegacyTranslationCache() {
        preferences.deleteKey(Preferences.Keys.APP_LANGUAGE_TRANSLATIONS)
        preferences.deleteKey(Preferences.Keys.APP_LANGUAGE_TRANSLATIONS_FETCHED_AT)
    }

    /**
     * Loads the JSON blob persisted by the most recent successful translations
     * response for [language]. Returns `true` only if [setLanguages] completes
     * without throwing AND the blob covers [language].
     */
    private fun tryLoadCachedLanguages(language: String): Boolean {
        val cached = preferences.getString(Preferences.Keys.translationsForLanguage(language), "")
        if (cached.isNullOrEmpty()) return false
        return try {
            setLanguages(cached, language)
            isLoadedFor(language)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Loads cached translations only when the persisted blob is younger than
     * [LANGUAGE_CACHE_TTL_MS]. Returns `true` on a successful in-window hit.
     */
    private fun loadFreshCachedLanguages(language: String): Boolean {
        val fetchedAt = preferences.getLong(
            Preferences.Keys.translationsFetchedAtForLanguage(language),
            0L
        )
        if (fetchedAt <= 0L) return false
        val age = System.currentTimeMillis() - fetchedAt
        if (age !in 0 until LANGUAGE_CACHE_TTL_MS) return false
        return tryLoadCachedLanguages(language)
    }

    /**
     * Returns the `misc/config-list` payload, served from memory unless [forceRefresh] is set.
     */
    suspend fun getConfigListAsync(forceRefresh: Boolean = false): ConfigList? {
        if (!forceRefresh) configList?.let { return it }
        val response: ConfigListResponse = awaitCallback { ok, fail ->
            TRPCore.core.trpRest.getConfigList(success = ok, error = fail)
        }
        configList = response.data
        return response.data
    }

    /**
     * Mapbox access token served by `misc/config-list`, cached as described in [getConfigSecret].
     */
    suspend fun getMapboxAccessToken(): String? = getConfigSecret(MAPBOX_TOKEN)

    /**
     * Last Mapbox access token persisted from `misc/config-list`, regardless of its age,
     * or `null` when none was stored.
     */
    fun getPersistedMapboxAccessToken(): String? = getPersistedConfigSecret(MAPBOX_TOKEN)

    /**
     * Mapbox access token persisted from `misc/config-list` when it is younger than
     * [CONFIG_SECRET_CACHE_TTL_MS], otherwise `null`.
     */
    fun getFreshPersistedMapboxAccessToken(): String? =
        getPersistedConfigSecret(MAPBOX_TOKEN)?.takeIf { isConfigSecretFresh(MAPBOX_TOKEN) }

    /**
     * Google Places API key served by `misc/config-list`, cached as described in [getConfigSecret].
     */
    suspend fun getGooglePlacesApiKey(): String? = getConfigSecret(GOOGLE_PLACES_KEY)

    /**
     * Last Google Places API key persisted from `misc/config-list`, regardless of its age,
     * or `null` when none was stored.
     */
    fun getPersistedGooglePlacesApiKey(): String? = getPersistedConfigSecret(GOOGLE_PLACES_KEY)

    /**
     * Returns [secret] from `misc/config-list`. A persisted value younger than
     * [CONFIG_SECRET_CACHE_TTL_MS] is returned without a request; otherwise the config list is
     * fetched from the network, bypassing the in-memory copy, and every secret it carries is
     * persisted. When that request fails or exceeds [CONFIG_FETCH_TIMEOUT_MS], or the response
     * lacks [secret], the expired persisted value is returned instead. Returns `null` when
     * neither exists.
     */
    private suspend fun getConfigSecret(secret: ConfigSecret): String? {
        val cachedValue = getPersistedConfigSecret(secret)
        if (cachedValue != null && isConfigSecretFresh(secret)) return cachedValue
        val fetchedConfigList = try {
            withTimeoutOrNull(CONFIG_FETCH_TIMEOUT_MS) {
                getConfigListAsync(forceRefresh = true)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            return cachedValue
        } ?: return cachedValue
        persistConfigSecrets(fetchedConfigList)
        return secret.extract(fetchedConfigList)?.takeIf { it.isNotBlank() } ?: cachedValue
    }

    private fun getPersistedConfigSecret(secret: ConfigSecret): String? {
        return preferences.getString(secret.valueKey, "")
            .takeIf { it.isNotBlank() }
    }

    private fun persistConfigSecrets(configList: ConfigList) {
        val fetchedAt = now()
        CONFIG_SECRETS.forEach { secret ->
            val value = secret.extract(configList)?.takeIf { it.isNotBlank() } ?: return@forEach
            preferences.setString(secret.valueKey, value)
            preferences.setLong(secret.fetchedAtKey, fetchedAt)
        }
    }

    private fun isConfigSecretFresh(secret: ConfigSecret): Boolean {
        val fetchedAt = preferences.getLong(secret.fetchedAtKey, 0L)
        if (fetchedAt <= 0L) return false
        val age = now() - fetchedAt
        return age in 0 until CONFIG_SECRET_CACHE_TTL_MS
    }

    /**
     * A credential served by `misc/config-list` and persisted under [valueKey] with its fetch
     * time under [fetchedAtKey].
     */
    private class ConfigSecret(
        val valueKey: String,
        val fetchedAtKey: String,
        val extract: (ConfigList) -> String?
    )

    /**
     * Parses a v2 translations payload, which carries only [requestedLanguage]
     * (or the server-side fallback when that language is unknown).
     */
    private fun setLanguages(jsonText: String, requestedLanguage: String) {
        if (jsonText.isEmpty()) return
        val data = JSONObject(jsonText).getJSONObject("data")
        languageValues = data.getJSONObject("translations")
        val langCodesJson = data.getJSONArray("langCodes")
        languageCodes = (0 until langCodesJson.length()).map { index ->
            val item = langCodesJson.getJSONObject(index)
            Pair(item.getString("value"), item.getString("label"))
        }
        setCurrentLanguageKeys(requestedLanguage)
        setDaysTexts()
        loadedLanguage = requestedLanguage
        isLanguagesLoaded = true
    }

    private fun setCurrentLanguageKeys(requestedLanguage: String) {
        val values = languageValues ?: return
        val resolvedLang = resolveLanguageCode(values, requestedLanguage)
        val changed = resolvedLang != _appliedLanguage.value
        TRPCore.core.appConfig.appLanguage = resolvedLang
        currentLanguageValues = values.getJSONObject(resolvedLang).getJSONObject("keys")
        if (changed) _appliedLanguage.postValue(resolvedLang)
    }

    /**
     * Resolves a language code to one the payload actually carries. Handles
     * null/empty values and regional locales (e.g., "es-MX" → "es"), then falls
     * back to the language the server returned.
     */
    private fun resolveLanguageCode(languageValues: JSONObject, langCode: String?): String {
        if (!langCode.isNullOrEmpty()) {
            if (languageValues.has(langCode)) return langCode
            val baseLang = langCode.split("-", "_").firstOrNull()?.lowercase()
            if (!baseLang.isNullOrEmpty() && languageValues.has(baseLang)) {
                return baseLang
            }
        }
        if (languageValues.has(DEFAULT_LANGUAGE)) return DEFAULT_LANGUAGE
        return languageValues.keys().asSequence().firstOrNull() ?: DEFAULT_LANGUAGE
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

    /**
     * Persists [lang]. Since a payload only carries one language, switching to a
     * language other than the loaded one invalidates the in-memory table and
     * pulls the new one in the background; the previous keys stay readable until
     * it arrives.
     */
    fun changeLanguage(lang: String) {
        if (lang.isBlank()) return
        val language = normalizeLanguage(lang)
        preferences.setString(Preferences.Keys.APP_LANGUAGE, language)
        if (loadedLanguage == language) {
            if (isLanguagesLoaded) runCatching { setCurrentLanguageKeys(language) }
            return
        }
        isLanguagesLoaded = false
        scope.launch { runCatching { getLanguageValuesAsync() } }
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

    fun getSavedLanguage(): String {
        return preferences.getString(Preferences.Keys.APP_LANGUAGE, "") ?: ""
    }

    fun getSavedCurrency(): String {
        return preferences.getString(Preferences.Keys.APP_CURRENCY, "") ?: ""
    }

    fun getLanguageValueForKey(key: String): String {
        if (key.isEmpty()) return ""
        val values = currentLanguageValues ?: return key
        return try {
            getNestedValue(values, key)
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
        val values = currentLanguageValues ?: return key
        return try {
            val translatedText = getNestedValue(values, key).replace("%s", "%S")
            String.format(translatedText, *texts.toTypedArray())
        } catch (_: Exception) {
            key
        }
    }

    companion object {
        /** Cold starts within this window load translations from preferences and skip the API. */
        private const val LANGUAGE_CACHE_TTL_MS: Long = 60L * 60L * 1000L

        /** A persisted config-list secret younger than this window is used without asking the API. */
        private const val CONFIG_SECRET_CACHE_TTL_MS: Long = 24L * 60L * 60L * 1000L

        /** Upper bound for the config-list request that refreshes the persisted secrets. */
        internal const val CONFIG_FETCH_TIMEOUT_MS: Long = 10_000L

        private val MAPBOX_TOKEN = ConfigSecret(
            valueKey = Preferences.Keys.MAPBOX_ACCESS_TOKEN,
            fetchedAtKey = Preferences.Keys.MAPBOX_ACCESS_TOKEN_FETCHED_AT,
            extract = { it.mapboxAccessToken }
        )

        private val GOOGLE_PLACES_KEY = ConfigSecret(
            valueKey = Preferences.Keys.GOOGLE_PLACES_API_KEY,
            fetchedAtKey = Preferences.Keys.GOOGLE_PLACES_API_KEY_FETCHED_AT,
            extract = { it.googlePlacesApiKey }
        )

        private val CONFIG_SECRETS = listOf(MAPBOX_TOKEN, GOOGLE_PLACES_KEY)

        private const val DEFAULT_LANGUAGE = "en"
    }
}
