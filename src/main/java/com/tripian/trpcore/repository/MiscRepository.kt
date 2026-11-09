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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.ResponseBody
import org.json.JSONObject
import javax.inject.Inject

class MiscRepository @Inject constructor(
    var app: Application,
    val preferences: Preferences
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

    /**
     * Guards [inflight] and every state commit of a fetch. At most one fetch is
     * active at a time; callers asking for the same language await it.
     */
    private val fetchMutex = Mutex()
    private var inflight: Deferred<Boolean>? = null
    private var inflightLanguage: String? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Returns the raw v2 translations payload for a language; replaceable in unit tests. */
    internal var translationsFetcher: suspend (String) -> String = { language ->
        fetchTranslationsFromApi(language)
    }

    /** Reports whether a network connection is available; replaceable in unit tests. */
    internal var isOnline: () -> Boolean = { app.isConnectedNet() }

    /**
     * Fetches the translation blob for the active language.
     * Concurrent callers requesting the same language share one in-flight request.
     */
    suspend fun getLanguageValuesAsync(): Boolean {
        val language = activeLanguage()
        if (isLoadedFor(language)) return true
        if (loadFreshCachedLanguages(language)) return true
        if (!isOnline()) return tryLoadCachedLanguages(language)
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
     * Forces a fresh translations fetch: the in-flight request, if any, is
     * cancelled and replaced (still respects the fresh-cache TTL).
     */
    suspend fun refetchLanguagesAsync(): Boolean {
        val language = activeLanguage()
        if (isLoadedFor(language)) return true
        if (loadFreshCachedLanguages(language)) return true
        if (!isOnline()) return tryLoadCachedLanguages(language)
        return runOrJoinFetch(language, replaceInflight = true)
    }

    /**
     * Second attempt after a timed-out or failed wait: joins the fetch still in
     * flight for the active language and starts a new one only when none is.
     */
    internal suspend fun retryLanguagesAsync(): Boolean {
        return waitForLanguagesLoadedAsync()
    }

    /**
     * Serves the active language from a cache entry younger than the TTL without
     * any network request. The cache is read and parsed on [scope], so the caller
     * never blocks; the result is committed under [fetchMutex] only while that
     * language is still active, not yet loaded and not being fetched.
     */
    internal fun warmUpFromCache(): Job = scope.launch {
        runCatching { warmUpActiveLanguageFromCache() }
    }

    private suspend fun warmUpActiveLanguageFromCache() {
        val language = activeLanguage()
        if (isLoadedFor(language)) return
        val cached = freshCachedTranslations(language) ?: return
        val parsed = parseTranslations(cached)
        fetchMutex.withLock {
            if (isLoadedFor(language) || language != activeLanguage()) return
            if (inflightLanguage == language && inflight?.isActive == true) return
            applyTranslations(parsed, language)
        }
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

    /**
     * Joins the active fetch for [language] or starts one. A fetch for another
     * language, or any fetch when [replaceInflight] is set, is cancelled first.
     */
    private suspend fun runOrJoinFetch(language: String, replaceInflight: Boolean = false): Boolean {
        val deferred = fetchMutex.withLock {
            if (isLoadedFor(language)) return true
            val existing = inflight
            if (!replaceInflight && existing != null && existing.isActive && inflightLanguage == language) {
                existing
            } else {
                existing?.cancel()
                scope.async { performFetch(language) }.also {
                    inflight = it
                    inflightLanguage = language
                }
            }
        }
        return awaitFetch(deferred)
    }

    /**
     * Awaits [deferred]. When it is cancelled by a replacing fetch while the
     * caller is still active, the caller follows the replacement instead.
     */
    private suspend fun awaitFetch(deferred: Deferred<Boolean>): Boolean {
        var current = deferred
        while (true) {
            try {
                return current.await()
            } catch (cancelled: CancellationException) {
                currentCoroutineContext().ensureActive()
                current = fetchMutex.withLock { inflight?.takeIf { it !== current && it.isActive } }
                    ?: return isLoadedFor(activeLanguage())
            }
        }
    }

    /**
     * Fetches [language] and commits it under [fetchMutex] only while this fetch
     * is still active and [language] is still the active language, so a
     * superseded fetch never overwrites the state of its replacement.
     */
    private suspend fun performFetch(language: String): Boolean {
        val jsonText = try {
            translationsFetcher(language)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            null
        }
        return fetchMutex.withLock {
            currentCoroutineContext().ensureActive()
            if (language != activeLanguage()) return@withLock isLoadedFor(activeLanguage())
            val applied = jsonText != null && applyFetchedTranslations(jsonText, language)
            applied || tryLoadCachedLanguages(language)
        }
    }

    private fun applyFetchedTranslations(jsonText: String, language: String): Boolean {
        return try {
            setLanguages(jsonText, language)
            if (isLoadedFor(language)) cacheTranslations(language, jsonText)
            isLoadedFor(language)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * The endpoint resolves the language from the request query TRPOne appends,
     * so the config language is aligned with [language] before the call.
     */
    private suspend fun fetchTranslationsFromApi(language: String): String {
        TRPCore.core.appConfig.appLanguage = language
        val body: ResponseBody = awaitCallback { ok, fail ->
            TRPCore.core.trpRest.getLanguageValuesV2(success = ok, error = fail)
        }
        return body.string()
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
        return applyCachedTranslations(cached, language)
    }

    private fun applyCachedTranslations(cached: String, language: String): Boolean {
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
        val cached = freshCachedTranslations(language) ?: return false
        return applyCachedTranslations(cached, language)
    }

    /** Returns the persisted blob for [language] when it is younger than [LANGUAGE_CACHE_TTL_MS]. */
    private fun freshCachedTranslations(language: String): String? {
        val fetchedAt = preferences.getLong(
            Preferences.Keys.translationsFetchedAtForLanguage(language),
            0L
        )
        if (fetchedAt <= 0L) return null
        val age = System.currentTimeMillis() - fetchedAt
        if (age !in 0 until LANGUAGE_CACHE_TTL_MS) return null
        return preferences.getString(Preferences.Keys.translationsForLanguage(language), "")
            .takeIf { it.isNotEmpty() }
    }

    suspend fun getConfigListAsync(): ConfigList? {
        configList?.let { return it }
        val response: ConfigListResponse = awaitCallback { ok, fail ->
            TRPCore.core.trpRest.getConfigList(success = ok, error = fail)
        }
        configList = response.data
        return response.data
    }

    /**
     * Parses a v2 translations payload, which carries only [requestedLanguage]
     * (or the server-side fallback when that language is unknown).
     */
    private fun setLanguages(jsonText: String, requestedLanguage: String) {
        if (jsonText.isEmpty()) return
        applyTranslations(parseTranslations(jsonText), requestedLanguage)
    }

    private class ParsedTranslations(
        val values: JSONObject,
        val codes: List<Pair<String, String>>
    )

    private fun parseTranslations(jsonText: String): ParsedTranslations {
        val data = JSONObject(jsonText).getJSONObject("data")
        val langCodesJson = data.getJSONArray("langCodes")
        val codes = (0 until langCodesJson.length()).map { index ->
            val item = langCodesJson.getJSONObject(index)
            Pair(item.getString("value"), item.getString("label"))
        }
        return ParsedTranslations(data.getJSONObject("translations"), codes)
    }

    private fun applyTranslations(parsed: ParsedTranslations, requestedLanguage: String) {
        languageValues = parsed.values
        languageCodes = parsed.codes
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
        private const val LANGUAGE_CACHE_TTL_MS: Long = 24L * 60L * 60L * 1000L

        private const val DEFAULT_LANGUAGE = "en"
    }
}
