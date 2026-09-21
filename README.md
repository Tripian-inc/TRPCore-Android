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
- [Jetpack Compose Integration](#jetpack-compose-integration)
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
    implementation 'com.github.Tripian-inc:TRPCore-Android:civitatis-2.0.0'
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

Use `startWithItinerary` method to start the SDK. This is the **only entry point** you need.

### Scenario 1: Create New Timeline

When user doesn't have an existing timeline, pass `tripHash = null`:

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
    tripHash = null,           // null = create new timeline
    uniqueId = "user_12345",
    canBack = true,
    appLanguage = "en"
)
```

### Scenario 2: Load Existing Timeline

When user has a previously saved `tripHash`, pass it along with the itinerary:

```kotlin
val itinerary = ItineraryWithActivities(
    tripName = "Barcelona Trip",
    startDatetime = "2025-03-15 09:00",
    endDatetime = "2025-03-18 18:00",
    uniqueId = "user_12345",
    tripianHash = savedTripHash,    // Can also pass here
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
    tripHash = savedTripHash,       // Or pass here - both work
    uniqueId = "user_12345",
    canBack = true,
    appLanguage = "en"
)
```

### startWithItinerary Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `context` | `Context` | Yes | Android context (see note below) |
| `itinerary` | `ItineraryWithActivities` | Yes | Trip data model |
| `tripHash` | `String?` | No | Existing timeline hash (null to create new) |
| `uniqueId` | `String?` | No | User ID (uses device ID if null) |
| `canBack` | `Boolean` | No | Show back button (default: true) |
| `appLanguage` | `String` | No | Language code (default: "en") |

> **Note:** `tripHash` can be passed either as a parameter or inside `itinerary.tripianHash`. If both are provided, the parameter takes precedence.

> **Important - Context Parameter:**
> - **Activity context** (`this`): SDK runs in the same task as your app. Back navigation works properly. **Recommended for Jetpack Compose apps.**
> - **Application context**: SDK runs in a new task. Use this only when launching from non-Activity contexts (e.g., Service, BroadcastReceiver).

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
    val activityId: String? = null,         // Activity ID (e.g., "15423")
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
    // activityId: Activity ID (e.g., "15423") or product ID
    Log.d("SDK", "Activity detail requested: $activityId")

    // Open your activity detail screen
    val intent = Intent(this, ActivityDetailActivity::class.java)
    intent.putExtra("activityId", activityId)
    startActivity(intent)
}
```

#### onRequestActivityReservation(activityId: String, date: String?)

Called when user taps "Reserve" or "Book" button. Start your booking flow.

```kotlin
override fun onRequestActivityReservation(activityId: String, date: String?) {
    Log.d("SDK", "Reservation requested: $activityId on $date")

    // date: Activity start in "yyyy-MM-dd HH:mm" format (e.g., "2026-04-17 09:00")
    // A flexible activity has no slot and reports 00:00
    // Null if the activity carries no usable date

    // Start your reservation/booking flow
    val intent = Intent(this, BookingActivity::class.java)
    intent.putExtra("activityId", activityId)
    date?.let { intent.putExtra("date", it) }
    startActivity(intent)
}
```

> **Note:** The `date` parameter is nullable with a default value of `null`. Existing implementations without the date parameter will continue to work (backward compatible).

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

#### onActivityAdded(activityId: String)

Called when an activity is successfully added to the timeline. Optional callback.

```kotlin
override fun onActivityAdded(activityId: String) {
    Log.d("SDK", "Activity added: $activityId")
    // Track manually added activities
}
```

### Listener Methods Summary

| Method | Required | Description |
|--------|----------|-------------|
| `onRequestActivityDetail(activityId)` | Yes | User tapped activity card - open detail screen |
| `onRequestActivityReservation(activityId, date?)` | Yes | User tapped Reserve - start booking flow (date format: "yyyy-MM-dd HH:mm", null if not available) |
| `onTimelineCreated(tripHash)` | Yes | **Save this hash** to load timeline later |
| `onTimelineLoaded(tripHash)` | No | Timeline loaded successfully |
| `onError(error)` | No | Error occurred in SDK |
| `onSDKDismissed()` | No | User exited the SDK |
| `onActivityAdded(activityId)` | No | Activity added to timeline |

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

    override fun onRequestActivityReservation(activityId: String, date: String?) {
        // Start your booking flow
        // date: Activity start in "yyyy-MM-dd HH:mm" format (e.g., "2026-04-17 09:00"), null if not available
        val message = if (date != null) {
            "Start booking for: $activityId on $date"
        } else {
            "Start booking for: $activityId"
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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

    override fun onActivityAdded(activityId: String) {
        // Activity added to timeline
        Toast.makeText(this, "Activity added: $activityId", Toast.LENGTH_SHORT).show()
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

### startWithTripHash

Use only when you have a tripHash but no itinerary data:

```kotlin
TRPCore.core.startWithTripHash(
    context = this,
    tripHash = "abc123xyz",
    uniqueId = "user_12345",
    canBack = true,
    appLanguage = "en"
)
```

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

## Jetpack Compose Integration

TRPCore ships a native Compose entry point for Single-Activity hosts: `TimelineNexus`. It renders the whole Timeline flow (timeline, add-plan wizard, POI/activity listings, POI detail, saved plans, starting point picker) as composables inside **your** `NavHost`, so the SDK never launches an Activity and your back stack stays intact.

### SDK screens inside your NavHost (full navigation control)

`NavGraphBuilder.tripianTimeline(...)` adds the Timeline flow to **your** graph as a nested graph with route `TimelineRoutes.GRAPH` (`"tripian"`). Every SDK screen is then a destination of your own `NavController`: you open the flow with a plain `navigate`, push your screens on top from the SDK callbacks, and system back pops SDK screens one by one until the Timeline root asks to close through `onDismiss`.

```kotlin
import com.tripian.trpcore.ui.timeline.compose.nav.TimelineRequest
import com.tripian.trpcore.ui.timeline.compose.nav.TimelineRoutes
import com.tripian.trpcore.ui.timeline.compose.nav.tripianTimeline

NavHost(navController, startDestination = "home") {
    composable("home") {
        HomeScreen(onOpenTimeline = { navController.navigate(TimelineRoutes.GRAPH) })
    }

    tripianTimeline(
        navController = navController,
        request = {
            tripStore.itinerary?.let { itinerary ->   // same ItineraryWithActivities as startWithItinerary
                TimelineRequest(
                    itinerary = itinerary,
                    tripHash = tripStore.tripHash,       // null creates a new timeline
                    uniqueId = tripStore.uniqueId,
                    appLanguage = "en",
                    appCurrency = "EUR"
                )
            }
        },
        onDismiss = { navController.popBackStack(TimelineRoutes.GRAPH, inclusive = true) }
    )

    composable("activity/{id}") { entry ->
        ActivityDetailScreen(entry.arguments?.getString("id"), onBack = { navController.popBackStack() })
    }
}
```

The graph is added once; `request` is read when the Timeline destination opens, so the trip can be filled in later (a null request pops the flow). To open a different itinerary later, pop the graph first (`popBackStack(TimelineRoutes.GRAPH, inclusive = true)`) and navigate again; the flow initializes once per graph entry. SDK routes are namespaced under `tripian/` and never clash with yours.

### Single composable entry point

`TimelineNexus` wraps the same graph in a NavHost of its own, for hosts that prefer one composable destination:

```kotlin
import com.tripian.trpcore.ui.timeline.compose.TimelineNexus

NavHost(navController, startDestination = "home") {
    composable("home") { HomeScreen(onOpenTimeline = { navController.navigate("timeline") }) }

    composable("timeline") {
        TimelineNexus(
            itinerary = itinerary,          // same ItineraryWithActivities as startWithItinerary
            tripHash = tripHash,            // null creates a new timeline
            uniqueId = uniqueId,
            appLanguage = "en",
            appCurrency = "EUR",
            onDismiss = { navController.popBackStack() }
        )
    }

    composable("activity/{id}") { entry ->
        ActivityDetailScreen(entry.arguments?.getString("id"), onBack = { navController.popBackStack() })
    }
}
```

Requirements:

- `TRPCore().init(...)` must have run (Application class) and `TRPCore.setListener(...)` must be set.
- Any `ComponentActivity` can host the flow: the SDK's sheets, pickers and dialogs are Compose, so no Fragment or AppCompat base class is required.
- The SDK pads its own screens for the status and navigation bars, so it works both full screen in an edge-to-edge host and inside a `Scaffold` whose content padding the host has consumed (`consumeWindowInsets`); nothing is applied twice.

### How navigation works

With `tripianTimeline` the SDK screens are entries of your back stack; with `TimelineNexus` they live in a nested `NavHost` whose state is kept by your `"timeline"` back-stack entry. In both cases the SDK screen state lives in ViewModels scoped to those entries. When an SDK callback (`onRequestActivityDetail`, `onRequestBookingDetail`, `onRequestActivityReservation`) makes you push one of **your** destinations on top, the `"timeline"` entry stays on your back stack. Popping your screen brings the user back to the exact SDK screen they left — for example the activity listing at the same scroll position, or the add-plan sheet on the same step.

Rules of thumb:

- Navigate to your screens with a plain `navController.navigate(route)`; do not `popUpTo` the timeline entry.
- Use `launchSingleTop` / `restoreState` when the timeline is a bottom-navigation tab so its state is restored on tab switches.
- To open a *different* itinerary, clear the entry first (`navController.clearBackStack("timeline")`) and navigate again; the flow initializes once per back-stack entry.
- System back inside the SDK pops the SDK's own screens first and calls `onDismiss` only from the timeline root.

### Handling SDK callbacks

```kotlin
object HostSdkListener : TRPCoreSDKListener {
    val requests = MutableSharedFlow<String>(extraBufferCapacity = 8)

    override fun onRequestActivityDetail(activityId: String) { requests.tryEmit("activity/$activityId") }
    override fun onRequestActivityReservation(activityId: String, date: String?) {
        requests.tryEmit("booking/$activityId?date=${date.orEmpty()}")
    }
    override fun onTimelineCreated(tripHash: String) { /* persist tripHash */ }
}

@Composable
fun App() {
    val navController = rememberNavController()
    LaunchedEffect(navController) {
        HostSdkListener.requests.collect { route -> navController.navigate(route) }
    }
    NavHost(navController, startDestination = "home") { /* ... */ }
}
```

### Activity entry point from Compose

`startWithItinerary()` still works from a Compose host. Pass the **Activity** context (not `applicationContext`) so the SDK Activities join your task and back navigation returns to your app:

| Context Type | Behavior | Use Case |
|--------------|----------|----------|
| **Activity** (`this`) | SDK runs in same task, back navigation works | Hosts that prefer the Activity flow |
| **Application** | SDK runs in new task, separate back stack | Service, BroadcastReceiver, background launches |

With the Activity flow, a host detail screen opened from an SDK callback must be an Activity of its own; a Compose destination in the host's `NavHost` would sit underneath the SDK Activity. Use `TimelineNexus` when the host is a Single-Activity Compose app.

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| "TRPCore not initialized" | Call `TRPCore().init()` in Application class |
| "destinationItems or tripItems required" | Provide at least one destination or activity |
| City not found | Provide correct `cityName` and `countryName` |
| Callbacks not received | Ensure `TRPCore.setListener(this)` is called |
| Back button doesn't return to SDK from Compose screen | Use `TimelineNexus`, or pass Activity context instead of Application context to `startWithItinerary()` |
| SDK opens in separate task (Compose apps) | Use `this` (Activity) instead of `applicationContext` when calling SDK methods |
| "LocalTimelineNavigator is not provided" | SDK composables must run inside `tripianTimeline` / `TimelineNexus`; do not call the screen composables directly |

---

## Version

Current version: **civitatis-2.0.0**

## Changelog

- **civitatis-2.0.0**: Native Compose integration. `NavGraphBuilder.tripianTimeline` adds the SDK screens to the host's own Navigation Compose graph and `TimelineNexus` offers the same flow as a single composable, so a Single-Activity host keeps its back stack: a host screen pushed from an SDK callback pops back to the exact SDK screen and state. The SDK no longer requires a `FragmentActivity` host and follows the host's own window insets, so full-screen and edge-to-edge hosts render correctly. City resolution ignores the host-provided `cityId` and derives the city from the activity id or its coordinates. The time sheets validate the picked span inline and warn when a place is closed at the selected hours.
- **civitatis-1.0.3**: `onRequestActivityReservation` now reports the activity's start time as well — its `date` format widens from "yyyy-MM-dd" to "yyyy-MM-dd HH:mm". A host parsing that string must be updated; a flexible activity reports the day at 00:00.
- **1.2.18**: Added date parameter to `onRequestActivityReservation` callback (format: "yyyy-MM-dd", backward compatible)
- **1.1.4**: Jetpack Compose integration support - conditional `FLAG_ACTIVITY_NEW_TASK` for proper back navigation when using Activity context
- **1.1.3**: Bottom toast alert, AddPlan UI fixes
- **1.1.2**: TEST environment support
- **1.1.0**: Optional destinationItems, cityName resolution
- **1.0.0**: Initial release

---

## Support

For issues and questions, please contact your Tripian representative.
