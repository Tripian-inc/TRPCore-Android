package com.tripian.trpcore.util.extensions

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Point
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Patterns
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.RelativeLayout
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsService
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.common.base.Splitter
import com.tripian.one.api.cities.model.City
import com.tripian.one.api.pois.model.Poi
import com.tripian.trpcore.util.Preferences
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseActivity
import java.net.InetAddress
import java.net.URLEncoder
import java.net.UnknownHostException
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

fun dp2Px(dp: Float): Float {
    val metrics = Resources.getSystem().displayMetrics
    val px = dp * (metrics.densityDpi / 160f)
    return px.roundToInt().toFloat()
}

fun EditText.showKeyboard() {
    try {
        requestFocus()
        post {
            val imm = this.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
            imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        }
    } catch (e: java.lang.Exception) {
        //Log.e("KeyBoardUtil", e.toString(), e);
    }
}

fun EditText.hideKeyboard() {
    try {
        val imm = this.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    } catch (e: java.lang.Exception) {
        //Log.e("KeyBoardUtil", e.toString(), e);
    }
}

fun FragmentActivity.hideKeyboard() {
    try {
        val view: View? = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    } catch (e: java.lang.Exception) {
    }
}

fun isValidEmail(target: CharSequence): Boolean {
    return !TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches()
}

fun isValidTCKN(identityNumber: String): Boolean {
    if (TextUtils.isEmpty(identityNumber)
        || identityNumber.length != 11
        || !identityNumber.matches("^([1-9]{1}[0-9]{10})$".toRegex())
    ) {
        return false
    }
    var oddNumberTotal = 0
    var evenNumberTotal = 0
    var total = 0
    var tenthNumber = 0
    var eleventhNumber = 0
    for (i in 0 until 11) {
        val charNumber = Character.getNumericValue(identityNumber.toCharArray()[i])
        if (i == 0 || i == 2 || i == 4 || i == 6 || i == 8) {
            oddNumberTotal += charNumber
            total += charNumber
        } else if (i == 1 || i == 3 || i == 5 || i == 7) {
            evenNumberTotal += charNumber
            total += charNumber
        } else if (i == 9) {
            tenthNumber = charNumber
            total += charNumber
        } else if (i == 10) {
            eleventhNumber = charNumber
        }
    }
    return (oddNumberTotal * 7 - evenNumberTotal) % 10 == tenthNumber && total % 10 == eleventhNumber && eleventhNumber % 2 == 0
}

fun clearTurkishChars(str: String?): String? {
    if (TextUtils.isEmpty(str)) {
        return str
    }

    var ret = str
    val turkishChars = charArrayOf(
        0x131.toChar(),
        0x130.toChar(),
        0xFC.toChar(),
        0xDC.toChar(),
        0xF6.toChar(),
        0xD6.toChar(),
        0x15F.toChar(),
        0x15E.toChar(),
        0xE7.toChar(),
        0xC7.toChar(),
        0x11F.toChar(),
        0x11E.toChar()
    )
    val englishChars = charArrayOf('i', 'I', 'u', 'U', 'o', 'O', 's', 'S', 'c', 'C', 'g', 'G')
    for (i in turkishChars.indices) {
        ret = ret!!.replace(
            String(charArrayOf(turkishChars[i])).toRegex(),
            String(charArrayOf(englishChars[i]))
        )
    }
    return ret
}

fun changeColor(fromColor: Int, toColor: Int, task: (Int) -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        val anim = ValueAnimator.ofFloat(0f, 1f)
        anim.addUpdateListener { animation ->
            // Use animation position to blend colors.
            val position = animation.animatedFraction

            task(blendColors(fromColor, toColor, position))
        }

        anim.setDuration(500).start()
    }
}

private fun blendColors(from: Int, to: Int, ratio: Float): Int {
    val inverseRatio = 1f - ratio

    val r = Color.red(to) * ratio + Color.red(from) * inverseRatio
    val g = Color.green(to) * ratio + Color.green(from) * inverseRatio
    val b = Color.blue(to) * ratio + Color.blue(from) * inverseRatio

    return Color.rgb(r.toInt(), g.toInt(), b.toInt())
}

fun getScreenSize(context: Context): IntArray {
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val display = windowManager.defaultDisplay
    val size = Point()
    if (Build.VERSION.SDK_INT > 16)
        display.getRealSize(size)
    else
        display.getSize(size)

    return intArrayOf(size.x, size.y)
}

fun getMetrics(context: Context): DisplayMetrics {
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val metrics = DisplayMetrics()
    wm.defaultDisplay.getMetrics(metrics)

    return metrics
}

fun checkConnection(context: Context?): Boolean {
    if (context != null) {
        val conMgr =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (conMgr != null) while (true) {
            val activeInfo = conMgr.activeNetworkInfo
            return activeInfo != null && activeInfo.isConnected
        }
    }

    return true
}

fun internetConnectionAvailable(timeOut: Int): Boolean {
    var inetAddress: InetAddress? = null
    try {
        val future =
            Executors.newSingleThreadExecutor().submit<InetAddress> {
                try {
                    return@submit InetAddress.getByName("google.com")
                } catch (e: UnknownHostException) {
                    return@submit null
                }
            }
        inetAddress = future[timeOut.toLong(), TimeUnit.MILLISECONDS]
        future.cancel(true)
    } catch (e: InterruptedException) {
    } catch (e: ExecutionException) {
    } catch (e: TimeoutException) {
    }

    return inetAddress != null && !inetAddress.equals("")
}

fun View.calculate(): IntArray {
    measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

    return intArrayOf(measuredWidth, measuredHeight)
}

fun getHourMinute(time: Long): String {
    var diffValue = ""
    val hour = time / (60 * 60 * 1000)
    var remaining = time % (60 * 60 * 1000).toLong()
    val minutes = remaining / (60 * 1000)
    remaining %= (60 * 1000).toLong()
    val seconds = remaining / 1000

    diffValue += if (hour > 0) {
        if (minutes > 0) {
            if (hour < 10) {
                "0$hour:"
            } else {
                "$hour:"
            }
        } else {
            if (hour < 10) {
                "0$hour:"
            } else {
                "$hour:"
            }
        }
    } else {
        "00:"
    }
    diffValue += if (minutes > 0) {
        if (seconds > 0) {
            if (minutes < 10) {
                "0$minutes:"
            } else {
                "$minutes:"
            }
        } else {
            if (minutes < 10) {
                "0$minutes:"
            } else {
                "$minutes:"
            }
        }
    } else {
        "00:"
    }
    diffValue += if (seconds > 0) {
        if (seconds < 10) {
            "0$seconds"
        } else {
            seconds.toString() + ""
        }
    } else {
        "00"
    }
    return diffValue
}

fun getUniqueId(context: Context): String {
    return try {
        Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
    } catch (e: Exception) {
        System.currentTimeMillis().toString() + ""
    }
}


//fun convertToLatLngBounds(southWest: String?, northEast: String?): LatLngBounds? {
//    val soundWestLatLng: LatLng? = convertToLatLng(southWest)
//    val northEastLatLng: LatLng? = convertToLatLng(northEast)
//    return if (soundWestLatLng == null || northEast == null) {
//        null
//    } else LatLngBounds(soundWestLatLng, northEastLatLng)
//}

//fun convertToLatLng(value: String?): LatLng? {
//    if (TextUtils.isEmpty(value)) {
//        return null
//    }
//
//    val split = Splitter.on(',').splitToList(value)
//    return if (split.size != 2) {
//        null
//    } else try {
//        LatLng(split[0].toDouble(), split[1].toDouble())
//    } catch (e: NullPointerException) {
//        null
//    } catch (e: NumberFormatException) {
//        null
//    }
//}

fun getLanguage(): String {
    return appLanguage
}

fun removeLastCharacter(str: String?): String? {
    var result: String? = null
    if (!str.isNullOrEmpty()) {
        result = str.substring(0, str.length - 1)
    }

    return result
}

fun checkEquality(s1: Array<Int?>?, s2: Array<Int?>?): Boolean {
    if (s1.contentEquals(s2)) {
        return true
    }

    if (s1 == null || s2 == null) {
        return false
    }
    val n = s1.size
    if (n != s2.size) {
        return false
    }
    for (i in 0 until n) {
        if (s1[i] != s2[i]) {
            return false
        }
    }
    return true
}

fun String?.generateImgLink(w: Int, h: Int): String? {
    val regex = "https://poi-pics.s3-eu-west-1.amazonaws.com"
    val replacement = "https://d2v9cz8rnpdl6f.cloudfront.net/"

    return this?.replace(regex.toRegex(), replacement + w + "x" + h)
}

fun View.getBitmap(): Bitmap {
    this.layoutParams = RelativeLayout.LayoutParams(
        RelativeLayout.LayoutParams.WRAP_CONTENT,
        RelativeLayout.LayoutParams.WRAP_CONTENT
    )
    this.measure(
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    )
    this.layout(0, 0, this.measuredWidth, this.measuredHeight)
    val bitmap = createBitmap(this.measuredWidth, this.measuredHeight)
    val c = Canvas(bitmap)
    this.layout(this.left, this.top, this.right, this.bottom)
    this.draw(c)

    return bitmap
}

fun Poi?.enableRating(): Boolean {
//    return if (poiCategoryList != null && poiCategoryList.size > 0) {
//        poiCategoryList.any { arrayOf("Restaurants", "Cafes", "Bars", "Bakeries", "Breweries", "Desserts").contains(it.name) }
//    } else {
//        false
//    }
    return true
}

fun City.isInCity(userLocation: Location?): Boolean {
    if (userLocation == null) return false

    if (boundary?.size != 4 && coordinate != null) {
        val distanceKm: Double = distance(
            userLocation.latitude, userLocation.longitude,
            coordinate!!.lat, coordinate!!.lng, 'K'
        )

        if (distanceKm < 50) {
            return true
        }
    }

    val es = LatLng(java.lang.Double.valueOf(boundary?.get(1) ?: 0.0), java.lang.Double.valueOf(boundary?.get(3) ?: 0.0))
    val nw = LatLng(java.lang.Double.valueOf(boundary?.get(0) ?: 0.0), java.lang.Double.valueOf(boundary?.get(2) ?: 0.0))
    var inLat = false
    var inLon = false
    if (nw.latitude > es.latitude) {
        if (nw.latitude > userLocation.latitude && userLocation.latitude > es.latitude) {
            inLat = true
        }
    } else {
        if (nw.latitude < userLocation.latitude && userLocation.latitude < es.latitude) {
            inLat = true
        }
    }
    if (nw.longitude > es.longitude) {
        if (nw.longitude > userLocation.longitude && userLocation.longitude > es.longitude) {
            inLon = true
        }
    } else {
        if (nw.longitude < userLocation.longitude && userLocation.longitude < es.longitude) {
            inLon = true
        }
    }

    return inLat && inLon
}

fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double, unit: Char): Double {
    val theta = lon1 - lon2
    var dist = sin(deg2rad(lat1)) * sin(deg2rad(lat2)) + cos(deg2rad(lat1)) * cos(deg2rad(lat2)) * cos(
        deg2rad(theta)
    )
    dist = acos(dist)
    dist = rad2deg(dist)
    dist *= 60 * 1.1515
    if (unit == 'K') {
        dist *= 1.609344
    } else if (unit == 'N') {
        dist *= 0.8684
    }

    return dist
}

fun deg2rad(deg: Double): Double {
    return deg * Math.PI / 180.0
}

fun rad2deg(rad: Double): Double {
    return rad * 180.0 / Math.PI
}

fun String.toLargeUrl(): String {
    return generateImgLink(800,600) ?: ""
}

fun String.toSmallUrl(): String {
    return generateImgLink(128,128) ?: ""
}

fun convertToLatLngBounds(southWest: String?, northEast: String?): LatLngBounds? {
    val soundWestLatLng: LatLng? = convertToLatLng(southWest)
    val northEastLatLng: LatLng? = convertToLatLng(northEast)
    return if (soundWestLatLng == null || northEast == null) {
        null
    } else LatLngBounds(soundWestLatLng, northEastLatLng)
}

fun convertToLatLng(value: String?): LatLng? {
    if (value.isNullOrEmpty()) {
        return null
    }

    val split = Splitter.on(',').splitToList(value)
    return if (split.size != 2) {
        null
    } else try {
        LatLng(split[0].toDouble(), split[1].toDouble())
    } catch (e: NullPointerException) {
        null
    } catch (e: NumberFormatException) {
        null
    }
}

private fun isHmsAvailable(context: Context?): Boolean {
    return false
//    var isAvailable = false
//    if (null != context) {
//        val result =
//            HuaweiApiAvailability.getInstance().isHuaweiMobileServicesAvailable(context)
//        isAvailable = com.huawei.hms.api.ConnectionResult.SUCCESS == result
//    }
//    return isAvailable
}

private fun isGmsAvailable(context: Context?): Boolean {
    var isAvailable = false
    if (null != context) {
        val result: Int = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
        isAvailable = com.google.android.gms.common.ConnectionResult.SUCCESS == result
    }
    return isAvailable
}

fun Float.roundTo(decimalPlaces: Int): Float {
    return "%.${decimalPlaces}f".format(Locale.ENGLISH, this).toFloat()
}

fun isHmsOnly(context: Context?) = isHmsAvailable(context) && !isGmsAvailable(context)

fun Context.deviceOs(): String {
    return if (isHmsOnly(this)) {
        "android-h"
    } else {
        "android"
    }
}

fun BaseActivity<*,*>.openCustomTabExt(url: String) {
    val activityInfo = getCustomTabBrowser()

    if (activityInfo != null) {
        try {
            val builder = CustomTabsIntent.Builder()
            val customTabsIntent = builder.build()

            val colorInt: Int = ContextCompat.getColor(this, R.color.trp_primary)
            val defaultColors = CustomTabColorSchemeParams.Builder()
                .setToolbarColor(colorInt)
                .build()
            builder.setDefaultColorSchemeParams(defaultColors)
            customTabsIntent.intent.setPackage(activityInfo.packageName)
            customTabsIntent.launchUrl(this, url.toUri())
        } catch (e: Exception) {
            openUrlExt(url)
        }
    } else {
        openUrlExt(url)
    }
}

fun BaseActivity<*,*>.openUrlExt(url: String) {
    val browserIntent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER)
    browserIntent.data = url.toUri()
    browserIntent.resolveActivity(packageManager)?.let {
        startActivity(browserIntent)
    }
}

private fun Context.getCustomTabBrowser(): ActivityInfo? {
    // Get default VIEW intent handler.
    val activityIntent = Intent()
        .setAction(Intent.ACTION_VIEW)
        .addCategory(Intent.CATEGORY_BROWSABLE)
        .setData(Uri.fromParts("http", "", null))

    // Get all apps that can handle VIEW intents.
    val resolvedActivityList = packageManager.queryIntentActivities(activityIntent, 0)

    for (info in resolvedActivityList) {
        val serviceIntent = Intent()
            .setAction(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION)
            .setPackage(info.activityInfo.packageName)
        // Check if this package also resolves the Custom Tabs service.
        if (packageManager.resolveService(serviceIntent, 0) != null) {
            return info.activityInfo
        }
    }

    return null
}

fun getDeviceId(pref: Preferences): String {
    val deviceId = pref.getString(Preferences.Keys.DEVICE_ID)

    if (!TextUtils.isEmpty(deviceId)) {
        return deviceId!!
    }

    val uid: String = try {
        UUID.randomUUID().toString()
    } catch (e: Exception) {
        System.currentTimeMillis().toString() + ""
    }

    pref.setString(Preferences.Keys.DEVICE_ID, uid)

    return uid
}

fun String?.encodeUrl(): String? {
    if (this.isNullOrEmpty()) null

    return URLEncoder.encode(this, "utf-8")
}
fun String.capitalizeFirstChar() = this.replaceFirstChar {
    if (it.isLowerCase()) it.titlecase(
        Locale.getDefault()
    ) else it.toString()
}


fun Context.isConnectedNet(): Boolean {
    var result = 0 // Returns connection type. 0: none; 1: mobile data; 2: wifi
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
    cm?.run {
        cm.getNetworkCapabilities(cm.activeNetwork)?.run {
            when {
                hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    result = 2
                }

                hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    result = 1
                }

                hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> {
                    result = 3
                }
            }
        }
    }

    return result != 0
}

fun Int.getCompactValue(): String {
    return if (abs(this / 1000000) >= 1) {
        (this / 1000000).toString() + "M"
    } else if (abs(this / 1000) >= 1) {
        (this / 1000).toString() + "K"
    } else {
        this.toString()
    }
}