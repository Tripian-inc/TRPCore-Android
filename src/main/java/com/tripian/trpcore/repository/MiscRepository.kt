package com.tripian.trpcore.repository

import android.app.Application
import com.tripian.gyg.base.Tripian
import com.tripian.one.api.misc.model.ConfigList
import com.tripian.trpcore.util.Preferences
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.appLanguage
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

    fun getLanguageValues(): Observable<Boolean> {
        if (app.isConnectedNet().not()) {
            setLanguages(preferences.getString(Preferences.Keys.APP_LANGUAGE_TRANSLATIONS, ""))
            return Observable.just(true)
        }
        return service.getLanguageValues().map {
            setLanguages(it.string())
            true
        }
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
    }

    private fun setCurrentLanguageKeys() {
        var currentLang = preferences.getString(Preferences.Keys.APP_LANGUAGE)
        if (currentLang.isNullOrEmpty()) {
            currentLang = "en"
        }

        appLanguage = currentLang
        currentLanguageValues = languageValues.getJSONObject(currentLang).getJSONObject("keys")
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

    fun getLanguageValueForKey(key: String): String {
        if (key.isEmpty()) return ""
        return try {
            currentLanguageValues.getString(key)
        } catch (_: Exception) {
            key
        }
    }

    fun getLanguageValueForKeyWithText(key: String, texts: List<String>): String {
        if (key.isEmpty()) return ""
        if (texts.isEmpty()) return getLanguageValueForKey(key)
        return try {
            val translatedText = currentLanguageValues.getString(key).replace("%s", "%S")

            return String.format(translatedText, *texts.toTypedArray())
        } catch (_: Exception) {
            key
        }
    }
}