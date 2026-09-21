# Moving the Timeline from an Activity to your Compose graph

**TRPCore Android — `civitatis-2.0.0`**

The SDK now renders every Timeline screen as composables inside your own `NavHost`. Your Single-Activity app keeps its back stack, and the SDK never starts an Activity of its own. Initialization, the listener and the itinerary payload are unchanged — only the entry point moves.

| | |
|---|---|
| Artifact | `com.github.Tripian-inc:TRPCore-Android` |
| Version | `civitatis-2.0.0` |
| Min SDK | 24 |
| Compile SDK | 35 |
| JVM target | 17 |

---

## What moves, what stays

**Changes — entry point and navigation**

- **Launch call.** `TRPCore.core.startWithItinerary(context, …)` becomes a destination in your graph.
- **Host screens.** Activity detail, booking detail and reservation screens opened from SDK callbacks can now be plain Compose destinations instead of separate Activities.
- **Back handling.** System back pops SDK screens one by one; the root asks you to close through `onDismiss`.
- **Host base class.** No `FragmentActivity` or AppCompat requirement — any `ComponentActivity` hosts the flow.

**Unchanged — everything else**

- **Initialization.** Same `TRPCore().init(...)` in your Application class, same four keys.
- **Listener.** Same `TRPCoreSDKListener`, same callbacks and signatures.
- **Trip payload.** Same `ItineraryWithActivities`, same `tripHash` / `uniqueId` semantics.
- **Repositories.** JitPack plus the Mapbox releases repo with your downloads token, as today.

> **The Activity entry point still ships.** `startWithItinerary()` works exactly as before in 2.0.0, so the move can be done on your schedule and rolled back without changing the dependency.

---

## 1. Update the dependency

Only the version string changes. The new entry points ship in the same artifact.

```gradle
dependencies {
    implementation 'com.github.Tripian-inc:TRPCore-Android:civitatis-2.0.0'
}
```

Your module must already build Compose. The SDK is compiled against Compose BOM 2024.09.03, `navigation-compose 2.7.7` and Kotlin 2.0.21 with the Compose compiler plugin; Gradle resolves to the higher version when yours is newer. Nothing else in your Gradle setup changes.

## 2. Keep initialization and the listener as they are

Both stay in your Application class. The Compose entry point asserts that `TRPCore().init(...)` has run before the Timeline composes.

```kotlin
TRPCore().init(
    app = this,
    tripianApiKey = "…",
    placesApiKey = "…",
    mapboxApiKey = "…",
    environment = Environment.PROD
)
TRPCore.setListener(SdkBridge)
```

## 3. Replace the launch call with a graph destination

Call `tripianTimeline(...)` once while building your `NavHost`. It installs the SDK as a nested graph on route `TimelineRoutes.GRAPH` (`"tripian"`); every SDK screen is then a destination of your own `NavController`. All SDK routes are namespaced under `tripian/` and cannot collide with yours.

**Before — Activity flow**

```kotlin
TRPCore.core.startWithItinerary(
    context = this,
    itinerary = itinerary,
    tripHash = savedTripHash,
    uniqueId = "user_12345",
    canBack = true,
    appLanguage = "en",
    appCurrency = "EUR"
)
```

**After — Compose flow**

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
            TimelineRequest(
                itinerary = itinerary,
                tripHash = savedTripHash,
                uniqueId = "user_12345",
                appLanguage = "en",
                appCurrency = "EUR"
            )
        },
        onDismiss = { navController.popBackStack(TimelineRoutes.GRAPH, inclusive = true) }
    )

    composable("activity/{id}") { entry ->
        ActivityDetailScreen(entry.arguments?.getString("id"))
    }
}
```

**Parameter mapping**

| `startWithItinerary` | `TimelineRequest` | Note |
|---|---|---|
| `context` | — | The flow composes in your Activity; there is no Activity to start. |
| `itinerary` | `itinerary` | Same `ItineraryWithActivities` you build today. |
| `tripHash` | `tripHash` | `null` creates a new timeline; a saved hash loads it. |
| `uniqueId` | `uniqueId` | Unchanged. |
| `canBack` | — | You own the back stack, so the flag is gone. |
| `appLanguage` / `appCurrency` | `appLanguage` / `appCurrency` | Unchanged. |

`request` is a lambda read when the Timeline destination opens, so the trip can still be loading when the graph is built; returning `null` pops the flow. To open a *different* itinerary later, pop the graph first and navigate again — the flow initializes once per graph entry.

### Alternative: one composable destination

If you would rather keep the SDK behind a single route, `TimelineNexus` wraps the same graph in a nested `NavHost`. Back-stack preservation works the same way; you just don't see the individual SDK routes.

```kotlin
import com.tripian.trpcore.ui.timeline.compose.TimelineNexus

composable("timeline") {
    TimelineNexus(
        itinerary = itinerary,
        tripHash = savedTripHash,
        uniqueId = "user_12345",
        appLanguage = "en",
        appCurrency = "EUR",
        onDismiss = { navController.popBackStack() }
    )
}
```

## 4. Turn SDK callbacks into navigation

The listener is a plain object with no Compose scope, so publish its requests and collect them where the `NavController` lives. Your detail screens are now ordinary destinations — they no longer have to be Activities.

```kotlin
object SdkBridge : TRPCoreSDKListener {

    private val _routes = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val routes: SharedFlow<String> = _routes

    override fun onRequestActivityDetail(activityId: String) {
        _routes.tryEmit("activity/$activityId")
    }

    override fun onRequestBookingDetail(bookingId: String) {
        _routes.tryEmit("booking/$bookingId")
    }

    override fun onRequestActivityReservation(activityId: String, date: String?) {
        _routes.tryEmit("reservation/$activityId?date=${date.orEmpty()}")
    }

    override fun onTimelineCreated(tripHash: String) { /* persist it */ }
}
```

```kotlin
LaunchedEffect(navController) {
    SdkBridge.routes.collect { route -> navController.navigate(route) }
}
```

`onRequestActivityReservation` reports the start time as well as the day: its `date` is `"yyyy-MM-dd HH:mm"`, and a flexible activity reports the day at `00:00`.

## 5. Place the flow in your chrome

The SDK pads its own screens for the status and navigation bars. Inside a `Scaffold`, consume the padding you apply so insets are not counted twice; full-screen edge-to-edge hosts need nothing at all.

```kotlin
Scaffold(bottomBar = { … }) { innerPadding ->
    val hostChrome = Modifier
        .padding(innerPadding)
        .consumeWindowInsets(innerPadding)

    NavHost(navController, startDestination = "home") { … }
}
```

The SDK inflates its remaining XML views under its own theme, so your app theme stays untouched — a Material 3 theme with no AppCompat parent is fine. If the manifest merger reports a conflict on `android:theme`, add `tools:replace="android:theme"` to your `<application>` tag.

---

## Navigation rules

| | |
|---|---|
| **Opening host screens** | Use a plain `navController.navigate(route)`. Do not `popUpTo` the Timeline entry — it is what holds the SDK's state. |
| **Coming back** | Popping your screen returns the user to the exact SDK screen they left: the activity list at the same scroll position, the add-plan sheet on the same step. |
| **Timeline as a tab** | Navigate with `launchSingleTop` and `restoreState = true` so its state survives tab switches. |
| **Switching itineraries** | Clear the entry first — `clearBackStack(route)`, or `popBackStack(TimelineRoutes.GRAPH, inclusive = true)` — then navigate again. The flow initializes once per back-stack entry. |
| **Closing the SDK** | System back pops SDK screens first; `onDismiss` fires only from the Timeline root, and your app decides where to go next. |

## If something looks off

| Symptom | Cause and fix |
|---|---|
| "LocalTimelineNavigator is not provided" | An SDK screen composable was called directly. Every SDK screen must run inside `tripianTimeline` or `TimelineNexus`. |
| "TRPCore is not initialized" | `TRPCore().init(...)` has not run yet — it belongs in `Application.onCreate()`, before any navigation. |
| "Either destinationItems or tripItems required" | The itinerary carries no location data. At least one destination or activity item with coordinates is required. |
| Back exits the app instead of returning to the Timeline | A host screen was opened with `popUpTo` over the Timeline entry, or through an Activity intent. Navigate within the same graph. |
| Double padding under the status bar | The host applied `Scaffold` padding without `consumeWindowInsets`. |
| Callbacks never arrive | `TRPCore.setListener(...)` was not called, or the listener was cleared in `onDestroy` of a screen that has since been recreated. |

## Migration checklist

- [ ] Bump the dependency to `civitatis-2.0.0` and sync.
- [ ] Leave `init()` and `setListener()` where they are.
- [ ] Add `tripianTimeline(...)` to your `NavHost` and delete the `startWithItinerary()` call.
- [ ] Route the three navigation callbacks to your own Compose destinations.
- [ ] Consume `Scaffold` insets around the graph if the SDK is not full screen.
- [ ] Verify back from an activity detail returns to the SDK screen and scroll position.
- [ ] Verify a second itinerary opens after clearing the Timeline entry.

---

Full API reference, itinerary payload and callback documentation: [README](README.md). Questions on the migration go to your Tripian contact.
