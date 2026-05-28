package com.tripian.trpcore.repository

import android.app.Application
import com.tripian.gyg.base.Tripian
import com.tripian.one.api.misc.model.ConfigList
import com.tripian.trpcore.base.TRPCore
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
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import org.json.JSONObject
import javax.inject.Inject

class MiscRepository @Inject constructor(
    var app: Application,
    val service: Service,
    val preferences: Preferences
) {
    private lateinit var languageValues: JSONObject
    private lateinit var currentLanguageValues: JSONObject
    var languageCodes: ArrayList<Pair<String, String>> = arrayListOf()

    private var configList: ConfigList? = null

    // Flag to track if languages have been loaded
    @Volatile
    var isLanguagesLoaded: Boolean = false
        private set

    // Subject to emit when languages are loaded - multiple subscribers can wait on this
    private val languagesLoadedSubject = BehaviorSubject.create<Boolean>()

    // Flag to track if a fetch is in progress
    @Volatile
    private var isFetchInProgress: Boolean = false

    /**
     * Fetches language values from API.
     * If called multiple times while a fetch is in progress, returns the same Observable.
     * This prevents multiple API calls and allows callers to wait for the ongoing fetch.
     */
    fun getLanguageValues(): Observable<Boolean> {
        // If already loaded, return immediately
        if (isLanguagesLoaded) {
            return Observable.just(true)
        }

        // Fresh-enough cache from a prior session — skip the network call. The
        // backend bundle changes rarely, so a 1-hour TTL keeps cold-starts fast
        // without serving badly stale strings.
        if (loadFreshCachedLanguages()) {
            return Observable.just(true)
        }

        // If offline, try to use cached data (any age — better than nothing).
        if (app.isConnectedNet().not()) {
            return Observable.just(tryLoadCachedLanguages())
        }

        // If fetch already in progress, return subject that will emit when done
        if (isFetchInProgress) {
            return languagesLoadedSubject.take(1)
        }

        // Start new fetch - runs on IO thread to avoid blocking main thread (ANR prevention)
        isFetchInProgress = true
        return service.getLanguageValues()
            .subscribeOn(Schedulers.io())
            .map {
                setLanguages(it.string())
                // Stamp the network success so subsequent cold starts within
                // LANGUAGE_CACHE_TTL_MS skip the request entirely.
                if (isLanguagesLoaded) {
                    preferences.setLong(
                        Preferences.Keys.APP_LANGUAGE_TRANSLATIONS_FETCHED_AT,
                        System.currentTimeMillis()
                    )
                }
                isLanguagesLoaded
            }
            // Network/server failure should not lock the user out if they have
            // previously loaded translations — fall back to the cached blob.
            .onErrorReturn { tryLoadCachedLanguages() }
            .doOnNext { success ->
                languagesLoadedSubject.onNext(success)
            }
            .doFinally {
                isFetchInProgress = false
            }
    }

    /**
     * Returns an Observable that emits when languages are loaded.
     * If already loaded, emits immediately.
     * If fetch is in progress, waits for it to complete.
     * If no fetch is in progress, starts one.
     */
    fun waitForLanguagesLoaded(): Observable<Boolean> {
        if (isLanguagesLoaded) {
            return Observable.just(true)
        }
        return getLanguageValues()
    }

    /**
     * Forces a fresh /languages fetch, bypassing the shared in-progress subject.
     * Callers that ended up with a stale `false` from a previous failed fetch
     * (or that timed out waiting on a still-in-flight init fetch) use this to
     * guarantee a definitive answer.
     */
    fun refetchLanguages(): Observable<Boolean> {
        if (isLanguagesLoaded) {
            return Observable.just(true)
        }
        // Even the "forced" path respects the fresh-cache TTL — the retry exists
        // for stuck/stale subject states, not to defeat the cache.
        if (loadFreshCachedLanguages()) {
            return Observable.just(true)
        }
        if (app.isConnectedNet().not()) {
            return Observable.just(tryLoadCachedLanguages())
        }
        isFetchInProgress = true
        return service.getLanguageValues()
            .subscribeOn(Schedulers.io())
            .map {
                setLanguages(it.string())
                // Stamp the network success so subsequent cold starts within
                // LANGUAGE_CACHE_TTL_MS skip the request entirely.
                if (isLanguagesLoaded) {
                    preferences.setLong(
                        Preferences.Keys.APP_LANGUAGE_TRANSLATIONS_FETCHED_AT,
                        System.currentTimeMillis()
                    )
                }
                isLanguagesLoaded
            }
            // Same fallback as the shared path: a network/server miss must not
            // close the SDK if cached translations from a prior session exist.
            .onErrorReturn { tryLoadCachedLanguages() }
            .doOnNext { success ->
                languagesLoadedSubject.onNext(success)
            }
            .doFinally {
                isFetchInProgress = false
            }
    }

    /**
     * Loads the JSON blob persisted by the most recent successful `/languages`
     * response from preferences. Returns `true` only if [setLanguages] completes
     * without throwing AND flips [isLanguagesLoaded]. Callers use this as a
     * last-resort fallback when the live fetch fails.
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
     * On a miss (no cache / stale / parse failure) the caller falls through to
     * the network path so a fresh bundle is fetched.
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

    fun getConfigList(): Observable<ConfigList> {
        return if (configList == null) {
            service.getConfigList().map {
                configList = it.data

                it.data
            }
        } else {
            Observable.just(configList)
        }
//        return service.getLanguageValues()
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
        Tripian.allText = getLanguageValueForKey("all")
        isLanguagesLoaded = true
    }

    private fun setCurrentLanguageKeys() {
        val currentLang = preferences.getString(Preferences.Keys.APP_LANGUAGE)

        // Resolve language code - handle null, empty, and regional locales like "es-MX" → "es"
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
        // Handle null or empty
        if (langCode.isNullOrEmpty()) {
            return "en"
        }

        // If the exact code exists, use it
        if (languageValues.has(langCode)) {
            return langCode
        }

        // Try base language code (e.g., "es-MX" → "es")
        val baseLang = langCode.split("-", "_").firstOrNull()?.lowercase()
        if (!baseLang.isNullOrEmpty() && languageValues.has(baseLang)) {
            return baseLang
        }

        // Fallback to English
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
        setCurrentLanguageKeys()
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

    /**
     * Gets the current currency code.
     * @return Current currency code (default: EUR)
     */
    fun getCurrentCurrency(): String {
        return TRPCore.core.appConfig.appCurrency
    }

    /**
     * Gets the saved currency code from preferences.
     * @return Saved currency code or empty string if not set
     */
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
     * e.g., "timeline.emptyState.addPlansButton" will navigate:
     * timeline -> emptyState -> addPlansButton
     *
     * Falls back to direct key lookup if nested navigation fails.
     */
    private fun getNestedValue(json: JSONObject, key: String): String {
        // First try direct key lookup (for flat structure)
        if (json.has(key)) {
            return json.getString(key)
        }

        // Try nested key navigation (for dot notation)
        val parts = key.split(".")
        if (parts.size == 1) {
            return json.getString(key)
        }

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

            return String.format(translatedText, *texts.toTypedArray())
        } catch (_: Exception) {
            key
        }
    }

    companion object {
        // 1 hour. Frontend translation bundle changes rarely, so a cold start
        // within this window can load from preferences and skip /languages.
        private const val LANGUAGE_CACHE_TTL_MS: Long = 60L * 60L * 1000L
    }
}