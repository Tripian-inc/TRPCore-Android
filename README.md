# TRPCore Android Library

TRPCore is the main Android library for integrating Tripian functionality into your Android application. It provides a comprehensive set of features for trip planning, management, and user interactions.

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

TRPCore uses Mapbox Android SDK v11, which requires the Mapbox Maven repository. Add it to your project-level `build.gradle` or `settings.gradle`:

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

**Note:** You need a Mapbox Downloads Token. Get it from your [Mapbox account](https://account.mapbox.com/access-tokens/). You can either:
- Set it as an environment variable: `MAPBOX_DOWNLOADS_TOKEN=your_token_here`
- Add it to your `local.properties`: `MAPBOX_DOWNLOADS_TOKEN=your_token_here`

### Step 3: Configure Mapbox Access Token

Create a resource file to store your Mapbox access token:

1. Create `res/values/mapbox_access_token.xml` in your app module:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:tools="http://schemas.android.com/tools">
    <string name="mapbox_access_token" translatable="false" tools:ignore="UnusedResources">YOUR_MAPBOX_ACCESS_TOKEN</string>
</resources>
```

2. Replace `YOUR_MAPBOX_ACCESS_TOKEN` with your actual Mapbox access token from [Mapbox account](https://account.mapbox.com/access-tokens/).

**Important:** 
- Keep this token secure and never commit it to version control
- Add `mapbox_access_token.xml` to `.gitignore` if it contains your production token
- Use different tokens for development and production

### Step 4: Add TRPCore dependency

In your app-level `build.gradle`:

```gradle
dependencies {
    implementation 'com.github.Tripian-inc:TRPCore-Android:1.1.3'
}
```

**Note:** TRPCore automatically includes the following dependencies:
- `com.github.Tripian-inc:TRPProviderKit-Android:1.0.0`
- `com.github.Tripian-inc:TRPOne-Android:1.0.0`
- `com.github.Tripian-inc:TRPAuth-Android:1.0.0`
- `com.github.Tripian-inc:TRPGyg-Android:1.0.0`
- `com.mapbox.maps:android-ndk27:11.16.0` (Mapbox Android SDK v11)

### Step 5: Minimum SDK Requirements

Ensure your `minSdkVersion` is set to at least 21 (Android 5.0 Lollipop) in your app's `build.gradle`:

```gradle
android {
    defaultConfig {
        minSdkVersion 21  // Required for Mapbox SDK v11
        // Other configurations...
    }
}
```

### Step 6: Sync Project

After making these changes, sync your project with Gradle files to apply the new configurations.

## Initialization

### Step 1: Initialize TRPCore in your Application class

You must initialize TRPCore in your `Application` class before using any of its features. The initialization should be done in the `onCreate()` method.

```kotlin
import android.app.Application
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.base.Environment

class MyApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize TRPCore
        TRPCore().init(
            app = this,
            placesApi = "YOUR_GOOGLE_PLACES_API_KEY",
            tripianApiKey = "YOUR_TRIPIAN_API_KEY",
            mapboxApiKey = "YOUR_MAPBOX_API_KEY",
            environment = Environment.PROD  // or Environment.DEV
        )
    }
}
```

### Step 2: Register your Application class

Don't forget to register your `Application` class in `AndroidManifest.xml`:

```xml
<application
    android:name=".MyApplication"
    ...>
    ...
</application>
```

### Step 3: Inject TRPCore into Activities (Optional)

If you need to use TRPCore's dependency injection in your activities, call the inject method:

```kotlin
import androidx.appcompat.app.AppCompatActivity
import com.tripian.trpcore.base.TRPCore

class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inject TRPCore dependencies
        TRPCore.inject(this)
        
        // Your code here
    }
}
```

## Initialization Parameters

### `init()` Method Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `app` | `Application` | Yes | Your application instance |
| `placesApi` | `String` | Yes | Google Places API key for location services |
| `tripianApiKey` | `String` | Yes | Your Tripian API key |
| `mapboxApiKey` | `String` | Yes | Mapbox API key for map functionality |
| `environment` | `Environment` | No | API environment (default: `Environment.PROD`) |

### Environment Options

- **`Environment.PROD`**: Production environment - uses `"prod/"` API version
- **`Environment.TEST`**: Test environment - uses `"test/"` API version
- **`Environment.DEV`**: Development environment - uses `"dev/"` API version
- **`Environment.PREDEV`**: Pre-development environment - uses `"predev/"` API version

**Example with DEV environment:**

```kotlin
TRPCore().init(
    app = this,
    placesApi = "YOUR_GOOGLE_PLACES_API_KEY",
    tripianApiKey = "YOUR_TRIPIAN_API_KEY",
    mapboxApiKey = "YOUR_MAPBOX_API_KEY",
    environment = Environment.DEV
)
```

## Starting Tripian

After initialization, you can start the Tripian experience using one of two methods:

### Method 1: Start with Email

Use this method when you have the user's email address:

```kotlin
import com.tripian.trpcore.base.TRPCore

// In your Activity or Fragment
TRPCore.core.startTripianWithEmail(
    context = this,
    email = "user@example.com",
    appLanguage = "en"  // Optional, default is "en"
)
```

**Parameters:**
- `context`: The application context (usually `this` from Activity/Fragment)
- `email`: User's email address (required)
- `appLanguage`: Language code for translations (optional, default: `"en"`)

### Method 2: Start with Unique ID

Use this method when you have a unique user identifier:

```kotlin
import com.tripian.trpcore.base.TRPCore

// In your Activity or Fragment
TRPCore.core.startTripianWithUniqueId(
    context = this,
    uniqueId = "user_unique_id_12345",
    appLanguage = "en"  // Optional, default is "en"
)
```

**Parameters:**
- `context`: The application context (usually `this` from Activity/Fragment)
- `uniqueId`: Unique user identifier (required)
- `appLanguage`: Language code for translations (optional, default: `"en"`)

## Complete Example

Here's a complete example showing the full integration:

```kotlin
import android.app.Application
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.base.Environment

// Application class
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        TRPCore().init(
            app = this,
            placesApi = "YOUR_GOOGLE_PLACES_API_KEY",
            tripianApiKey = "YOUR_TRIPIAN_API_KEY",
            mapboxApiKey = "YOUR_MAPBOX_API_KEY",
            environment = Environment.PROD
        )
    }
}

// Activity class
class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Inject TRPCore (optional)
        TRPCore.inject(this)
        
        // Example: Start Tripian with email
        findViewById<Button>(R.id.startTripianButton).setOnClickListener {
            TRPCore.core.startTripianWithEmail(
                context = this,
                email = "user@example.com",
                appLanguage = "en"
            )
        }
        
        // Example: Start Tripian with unique ID
        findViewById<Button>(R.id.startTripianWithIdButton).setOnClickListener {
            TRPCore.core.startTripianWithUniqueId(
                context = this,
                uniqueId = "user_unique_id_12345",
                appLanguage = "en"
            )
        }
    }
}
```

## Language Support

The `appLanguage` parameter supports various language codes. Common examples:

- `"en"` - English (default)
- `"tr"` - Turkish
- `"de"` - German
- `"fr"` - French
- `"es"` - Spanish

## Important Notes

1. **Initialization Order**: TRPCore must be initialized in your `Application.onCreate()` before calling any `startTripian` methods.

2. **Environment Selection**: Choose the appropriate environment:
   - Use `Environment.DEV` for development and testing
   - Use `Environment.PROD` for production releases

3. **API Keys**: Make sure to keep your API keys secure. Consider using environment variables or secure storage for production apps.

4. **Context**: Always use the application context or activity context when calling `startTripian` methods.

5. **Dependencies**: TRPCore automatically manages its dependencies. You don't need to manually add TRPProvider, TRPOne, TRPAuth, or TRPGyg as separate dependencies.

## Troubleshooting

### Issue: "TRPCore not initialized"
**Solution**: Make sure you've called `TRPCore().init()` in your `Application.onCreate()` method before using any TRPCore functionality.

### Issue: "Cannot find TRPCore.core"
**Solution**: Ensure that `TRPCore().init()` has been called successfully. The `core` companion object is initialized during the `init()` call.

### Issue: API calls failing
**Solution**: 
- Verify your API keys are correct
- Check that you're using the correct environment (DEV vs PROD)
- Ensure you have internet connectivity

## Version

Current version: **1.1.3**

## License

[Add your license information here]

## Support

For issues and questions, please contact [your support contact or repository issues page].

