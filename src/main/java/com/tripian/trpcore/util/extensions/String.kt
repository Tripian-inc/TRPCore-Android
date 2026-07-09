package com.tripian.trpcore.util.extensions

import android.graphics.Typeface
import android.net.Uri
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.core.net.toUri
import java.util.Locale

fun String.capitalized(): String {
    return this.replaceFirstChar {
        if (it.isLowerCase())
            it.titlecase(Locale.getDefault())
        else it.toString()
    }
}

fun getPriceSpannableString(color: Int, price: Int): SpannableStringBuilder {

    val str = SpannableStringBuilder("$$$$")
    str.setSpan(
        StyleSpan(Typeface.BOLD),
        0,
        Integer.valueOf(price),
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    str.setSpan(
        ForegroundColorSpan(color),
        0,
        Integer.valueOf(price),
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    return str
}

fun String.buildUrlWithParams(
    params: Map<String, String>
): String {
    var uri = this.toUri()

    params.forEach { (key, value) ->
        uri = uri.addQueryParameterIfAbsent(key, value)
    }
    return uri.toString()
}

fun String.containsAnyWord(words: List<String>): Boolean {
    return words.any { word -> this.contains(word, ignoreCase = true) }
}

/**
 * Converts a segment-level "yyyy-MM-dd HH:mm" datetime into the ISO-8601
 * "yyyy-MM-dd'T'HH:mm:ss" shape that `additionalData.startDatetime` /
 * `additionalData.endDatetime` expect. Seconds are always emitted as `:00`
 * because the source string never carries them.
 */
fun String.toAdditionalDataIso(): String = "$this:00".replace(' ', 'T')

fun Uri.addQueryParameterIfAbsent(key: String, value: String): Uri {
    if (this.getQueryParameter(key) != null) {
        return this
    }

    val builder = this.buildUpon().clearQuery()
    val existingParams = this.queryParameterNames

    for (param in existingParams) {
        val values = this.getQueryParameters(param)
        for (v in values) {
            builder.appendQueryParameter(param, v)
        }
    }

    builder.appendQueryParameter(key, value)
    return builder.build()
}