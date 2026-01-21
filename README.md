# TRPCore Android Library

TRPCore is the main Android library for integrating Tripian Timeline Itinerary functionality into your Android application. It provides a comprehensive set of features for trip planning, timeline management, and activity recommendations.

## Table of Contents

- [Installation](#installation)
- [Initialization](#initialization)
- [Starting Timeline Itinerary](#starting-timeline-itinerary)
- [Data Models](#data-models)
- [SDK Callbacks (Listener)](#sdk-callbacks-listener)
- [Complete Integration Example](#complete-integration-example)
- [Legacy Methods](#legacy-methods)
- [Language Support](#language-support)
- [Troubleshooting](#troubleshooting)

---

## Installation

Add TRPCore to your project using JitPack:

### Step 1: Add JitPack repository

In your project-level `build.gradle` (or `settings.gradle`):

```gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

### Step 2: Add Mapbox Maven repository

TRPCore uses Mapbox Android SDK v11, which requires the Mapbox Maven repository:

**For Groovy (build.gradle):**
```gradle
allprojects {
    repositories {
        ...
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            authentication {
                basic(BasicAuthentication)
            }
            credentials {
                username = "mapbox"
                password = project.findProperty("MAPBOX_DOWNLOADS_TOKEN") ?: System.getenv("MAPBOX_DOWNLOADS_TOKEN") ?: ""
            }
        }
    }
}
```

**For Kotlin DSL (settings.gradle.kts):**
```kotlin
dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            authentication {
                create<BasicAuthentication>("basic")
            }
            credentials {
                username = "mapbox"
                password = providers.gradleProperty("MAPBOX_DOWNLOADS_TOKEN")
                    .orElse(providers.environmentVariable("MAPBOX_DOWNLOADS_TOKEN"))
                    .orElse("")
                    .get()
            }
        }
    }
}
```

**Note:** You need a Mapbox Downloads Token from your [Mapbox account](https://account.mapbox.com/access-tokens/).

### Step 3: Add TRPCore dependency

In your app-level `build.gradle`:

```gradle
dependencies {
    implementation 'com.github.Tripian-inc:TRPCore-Android:1.1.3'
}
```

### Step 4: Minimum SDK Requirements

```gradle
android {
    defaultConfig {
        minSdkVersion 24  // Required for TRPCore
    }
}
```

---

## Initialization

### Initialize TRPCore in your Application class

```kotlin
import android.app.Application
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.base.Environment

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        TRPCore().init(
            app = this,
            tripianApiKey = "YOUR_TRIPIAN_API_KEY",
            placesApiKey = "YOUR_GOOGLE_PLACES_API_KEY",
            mapboxApiKey = "YOUR_MAPBOX_API_KEY",
            environment = Environment.PROD
        )
    }
}
```

### init() Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `app` | `Application` | Yes | Your application instance |
| `tripianApiKey` | `String` | Yes | Your Tripian API key |
| `placesApiKey` | `String` | Yes | Google Places API key |
| `mapboxApiKey` | `String` | Yes | Mapbox API key |
| `environment` | `Environment` | No | API environment (default: `PROD`) |

### Environment Options

| Environment | API Path | Usage |
|-------------|----------|-------|
| `Environment.PROD` | `"prod/"` | Production |
| `Environment.TEST` | `"test/"` | Testing |
| `Environment.DEV` | `"dev/"` | Development |
| `Environment.PREDEV` | `"predev/"` | Pre-development |

---

## Starting Timeline Itinerary

### Method 1: startWithItinerary (Recommended)

This is the **primary entry point** for the SDK. Use when you have user's travel information.

```kotlin
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.domain.model.itinerary.*

val itinerary = ItineraryWithActivities(
    tripName = "Barcelona Trip",
    startDatetime = "2025-03-15 09:00",
    endDatetime = "2025-03-18 18:00",
    uniqueId = "user_12345",
    destinationItems = listOf(
        SegmentDestinationItem(
            title = "Barcelona",
            coordinate = "41.3851,2.1734",
            countryName = "Spain"
        )
    )
)

TRPCore.core.startWithItinerary(
    context = this,
    itinerary = itinerary,
    tripHash = null,           // null = create new, or pass existing hash
    uniqueId = "user_12345",
    canBack = true,
    appLanguage = "en"
)
```

#### startWithItinerary Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `context` | `Context` | Yes | Android context |
| `itinerary` | `ItineraryWithActivities` | Yes | Trip data model |
| `tripHash` | `String?` | No | Existing timeline hash (null to create new) |
| `uniqueId` | `String?` | No | User ID (uses device ID if null) |
| `canBack` | `Boolean` | No | Show back button (default: true) |
| `appLanguage` | `String` | No | Language code (default: "en") |

### Method 2: startWithTripHash

Use when you have a previously created timeline hash:

```kotlin
TRPCore.core.startWithTripHash(
    context = this,
    tripHash = "abc123xyz",
    uniqueId = "user_12345",
    canBack = true,
    appLanguage = "en"
)
```

#### startWithTripHash Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `context` | `Context` | Yes | Android context |
| `tripHash` | `String` | Yes | Timeline hash to load |
| `uniqueId` | `String?` | No | User ID |
| `canBack` | `Boolean` | No | Show back button (default: true) |
| `appLanguage` | `String` | No | Language code (default: "en") |

---

## Data Models

### ItineraryWithActivities

Main model for starting the SDK.

```kotlin
data class ItineraryWithActivities(
    val tripName: String? = null,
    val startDatetime: String,                                 // "yyyy-MM-dd HH:mm"
    val endDatetime: String,                                   // "yyyy-MM-dd HH:mm"
    val uniqueId: String,
    val tripianHash: String? = null,
    val destinationItems: List<SegmentDestinationItem> = emptyList(),
    val favouriteItems: List<SegmentFavoriteItem>? = null,
    val tripItems: List<SegmentActivityItem>? = null
)
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `tripName` | `String?` | No | Display name for the trip |
| `startDatetime` | `String` | Yes | Format: "yyyy-MM-dd HH:mm" |
| `endDatetime` | `String` | Yes | Format: "yyyy-MM-dd HH:mm" |
| `uniqueId` | `String` | Yes | Unique user identifier |
| `tripianHash` | `String?` | No | Existing timeline hash |
| `destinationItems` | `List` | No* | List of destinations |
| `favouriteItems` | `List?` | No | User's favorite activities |
| `tripItems` | `List?` | No* | Booked/reserved activities |

> *Either `destinationItems` or `tripItems` must have at least one item.

### SegmentDestinationItem

```kotlin
data class SegmentDestinationItem(
    val title: String,                  // City name (e.g., "Barcelona")
    val coordinate: String,             // "lat,lng" format
    val cityId: Int? = null,            // Optional - SDK resolves from name
    val dates: List<String>? = null,    // Days in this city
    val countryName: String? = null     // Helps with city resolution
)
```

### SegmentActivityItem

Booked/reserved activities to display in timeline:

```kotlin
data class SegmentActivityItem(
    val activityId: String? = null,
    val bookingId: String? = null,
    val title: String? = null,
    val imageUrl: String? = null,
    val description: String? = null,
    val startDatetime: String? = null,      // "yyyy-MM-dd HH:mm"
    val endDatetime: String? = null,
    val coordinate: ItineraryCoordinate,
    val cancellation: String? = null,
    val adultCount: Int = 1,
    val childCount: Int = 0,
    val bookingUrl: String? = null,
    val duration: Double? = null,           // Minutes
    val price: SegmentActivityPrice? = null,
    val cityId: Int? = null,
    val cityName: String? = null,
    val countryName: String? = null
)
```

### ItineraryCoordinate

```kotlin
data class ItineraryCoordinate(
    val lat: Double,
    val lng: Double
)
```

### SegmentActivityPrice

```kotlin
data class SegmentActivityPrice(
    val currency: String,     // "EUR", "USD", etc.
    val value: Double
)
```

### SegmentFavoriteItem

User's favorite activities for Smart Recommendations:

```kotlin
data class SegmentFavoriteItem(
    val activityId: String? = null,         // Format: "C_15423_15"
    val title: String,
    val cityName: String,
    val cityId: Int? = null,
    val photoUrl: String? = null,
    val description: String? = null,
    val activityUrl: String? = null,
    val coordinate: ItineraryCoordinate,
    val rating: Double? = null,
    val ratingCount: Int? = null,
    val cancellation: String? = null,
    val duration: Double? = null,
    val price: SegmentActivityPrice? = null,
    val locations: List<String>? = null
)
```

---

## SDK Callbacks (Listener)

Implement `TRPCoreSDKListener` to receive events from the SDK. **This is essential for proper integration.**

### Setting Up the Listener

```kotlin
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.sdk.TRPCoreSDKListener

class MainActivity : AppCompatActivity(), TRPCoreSDKListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the SDK listener
        TRPCore.setListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear listener when activity is destroyed
        TRPCore.setListener(null)
    }
}
```

### Listener Methods

#### onRequestActivityDetail(activityId: String)

Called when user taps on an activity card. Open your activity detail screen.

```kotlin
override fun onRequestActivityDetail(activityId: String) {
    // activityId format: "C_15423_15" or product ID
    Log.d("SDK", "Activity detail requested: $activityId")

    // Open your activity detail screen
    val intent = Intent(this, ActivityDetailActivity::class.java)
    intent.putExtra("activityId", activityId)
    startActivity(intent)
}
```

#### onRequestActivityReservation(activityId: String)

Called when user taps "Reserve" or "Book" button. Start your booking flow.

```kotlin
override fun onRequestActivityReservation(activityId: String) {
    Log.d("SDK", "Reservation requested: $activityId")

    // Start your reservation/booking flow
    val intent = Intent(this, BookingActivity::class.java)
    intent.putExtra("activityId", activityId)
    startActivity(intent)
}
```

#### onTimelineCreated(tripHash: String)

**IMPORTANT:** Called when a new timeline is created. **You must save this hash** to access the timeline later.

```kotlin
override fun onTimelineCreated(tripHash: String) {
    Log.d("SDK", "Timeline created: $tripHash")

    // SAVE THIS HASH - Required to load the timeline again
    getSharedPreferences("app_prefs", MODE_PRIVATE)
        .edit()
        .putString("trip_hash", tripHash)
        .apply()

    // Or save to your backend
    apiService.saveTripHash(userId, tripHash)
}
```

#### onTimelineLoaded(tripHash: String)

Called when timeline is loaded (after fetch or create). Optional callback.

```kotlin
override fun onTimelineLoaded(tripHash: String) {
    Log.d("SDK", "Timeline loaded: $tripHash")
}
```

#### onError(error: String)

Called when an error occurs in the SDK. Optional callback.

```kotlin
override fun onError(error: String) {
    Log.e("SDK", "Error: $error")
    Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
}
```

#### onSDKDismissed()

Called when user exits the SDK (back pressed). Optional callback.

```kotlin
override fun onSDKDismissed() {
    Log.d("SDK", "SDK dismissed")
    // Perform any cleanup or navigation
}
```

### Listener Methods Summary

| Method | Required | Description |
|--------|----------|-------------|
| `onRequestActivityDetail(activityId)` | Yes | User tapped activity card - open detail screen |
| `onRequestActivityReservation(activityId)` | Yes | User tapped Reserve - start booking flow |
| `onTimelineCreated(tripHash)` | Yes | **Save this hash** to load timeline later |
| `onTimelineLoaded(tripHash)` | No | Timeline loaded successfully |
| `onError(error)` | No | Error occurred in SDK |
| `onSDKDismissed()` | No | User exited the SDK |

---

## Complete Integration Example

```kotlin
import android.app.Application
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.base.Environment
import com.tripian.trpcore.domain.model.itinerary.*
import com.tripian.trpcore.sdk.TRPCoreSDKListener

// ========================
// Application Class
// ========================
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        TRPCore().init(
            app = this,
            tripianApiKey = "YOUR_TRIPIAN_API_KEY",
            placesApiKey = "YOUR_GOOGLE_PLACES_API_KEY",
            mapboxApiKey = "YOUR_MAPBOX_API_KEY",
            environment = Environment.PROD
        )
    }
}

// ========================
// Activity Class
// ========================
class MainActivity : AppCompatActivity(), TRPCoreSDKListener {

    private var savedTripHash: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set SDK listener
        TRPCore.setListener(this)

        // Load saved trip hash
        savedTripHash = getSharedPreferences("app_prefs", MODE_PRIVATE)
            .getString("trip_hash", null)

        // Create new trip button
        findViewById<Button>(R.id.btnNewTrip).setOnClickListener {
            createNewTrip()
        }

        // Load existing trip button
        findViewById<Button>(R.id.btnLoadTrip).setOnClickListener {
            loadExistingTrip()
        }
    }

    private fun createNewTrip() {
        val itinerary = ItineraryWithActivities(
            tripName = "Barcelona Adventure",
            startDatetime = "2025-03-15 09:00",
            endDatetime = "2025-03-18 18:00",
            uniqueId = "user_12345",
            destinationItems = listOf(
                SegmentDestinationItem(
                    title = "Barcelona",
                    coordinate = "41.3851,2.1734",
                    countryName = "Spain"
                )
            ),
            // Optional: Add booked activities
            tripItems = listOf(
                SegmentActivityItem(
                    activityId = "ACT_001",
                    bookingId = "BOOK_789",
                    title = "Sagrada Familia Tour",
                    startDatetime = "2025-03-16 10:00",
                    endDatetime = "2025-03-16 12:00",
                    coordinate = ItineraryCoordinate(41.4036, 2.1744),
                    adultCount = 2,
                    price = SegmentActivityPrice("EUR", 65.0),
                    cityName = "Barcelona",
                    countryName = "Spain"
                )
            )
        )

        TRPCore.core.startWithItinerary(
            context = this,
            itinerary = itinerary,
            canBack = true,
            appLanguage = "en"
        )
    }

    private fun loadExistingTrip() {
        val tripHash = savedTripHash
        if (tripHash != null) {
            TRPCore.core.startWithTripHash(
                context = this,
                tripHash = tripHash,
                uniqueId = "user_12345",
                canBack = true,
                appLanguage = "en"
            )
        } else {
            Toast.makeText(this, "No saved trip found", Toast.LENGTH_SHORT).show()
        }
    }

    // ========================
    // SDK Listener Callbacks
    // ========================

    override fun onRequestActivityDetail(activityId: String) {
        // Open your activity detail screen
        Toast.makeText(this, "Open detail for: $activityId", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestActivityReservation(activityId: String) {
        // Start your booking flow
        Toast.makeText(this, "Start booking for: $activityId", Toast.LENGTH_SHORT).show()
    }

    override fun onTimelineCreated(tripHash: String) {
        // IMPORTANT: Save the trip hash!
        savedTripHash = tripHash
        getSharedPreferences("app_prefs", MODE_PRIVATE)
            .edit()
            .putString("trip_hash", tripHash)
            .apply()

        Toast.makeText(this, "Trip saved!", Toast.LENGTH_SHORT).show()
    }

    override fun onTimelineLoaded(tripHash: String) {
        // Timeline loaded
    }

    override fun onError(error: String) {
        Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
    }

    override fun onSDKDismissed() {
        // SDK closed
    }

    override fun onDestroy() {
        super.onDestroy()
        TRPCore.setListener(null)
    }
}
```

---

## Legacy Methods

These methods are available for backwards compatibility:

### startTripianWithEmail

```kotlin
TRPCore.core.startTripianWithEmail(
    context = this,
    email = "user@example.com",
    appLanguage = "en"
)
```

### startTripianWithUniqueId

```kotlin
TRPCore.core.startTripianWithUniqueId(
    context = this,
    uniqueId = "user_unique_id",
    appLanguage = "en"
)
```

---

## Language Support

| Code | Language |
|------|----------|
| `"en"` | English (default) |
| `"tr"` | Turkish |
| `"de"` | German |
| `"fr"` | French |
| `"es"` | Spanish |

---

## Important Notes

1. **Initialization**: Call `TRPCore().init()` in `Application.onCreate()` before any SDK methods.

2. **Save tripHash**: Always implement `onTimelineCreated` and save the hash to load the timeline later.

3. **Listener Lifecycle**: Set listener in `onCreate()` and clear in `onDestroy()`.

4. **City Resolution**: SDK can resolve `cityId` from `cityName` + `countryName` if not provided.

5. **Location Data**: Either `destinationItems` or `tripItems` must have at least one item.

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| "TRPCore not initialized" | Call `TRPCore().init()` in Application class |
| "destinationItems or tripItems required" | Provide at least one destination or activity |
| City not found | Provide correct `cityName` and `countryName` |
| Callbacks not received | Ensure `TRPCore.setListener(this)` is called |

---

## Version

Current version: **1.1.3**

## Changelog

- **1.1.3**: Bottom toast alert, AddPlan UI fixes
- **1.1.2**: TEST environment support
- **1.1.0**: Optional destinationItems, cityName resolution
- **1.0.0**: Initial release

---

## Support

For issues and questions, please contact your Tripian representative.
