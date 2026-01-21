# TRPCore Library - Consumer ProGuard Rules
# These rules are applied to all consumer projects that include TRPCore

# ============================================================================
# KEEP ANNOTATIONS
# ============================================================================

-keepattributes *Annotation*
-keepattributes Signature
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ============================================================================
# DAGGER DEPENDENCY INJECTION
# ============================================================================

-keep class com.tripian.trpcore.di.** { *; }
-keep @interface dagger.** { *; }
-keep class dagger.** { *; }

-keepclasseswithmembernames class * {
    @dagger.* <fields>;
    @dagger.* <methods>;
}

-keepclasseswithmembernames class * {
    @javax.inject.* <fields>;
    @javax.inject.* <methods>;
}

-keep class **Dagger* { *; }
-keep class **Factory { *; }
-keep class **Module { *; }

# ============================================================================
# RETROFIT + OKHTTP + GSON
# ============================================================================

-keep interface com.tripian.trpcore.repository.Service { *; }
-keep class com.tripian.trpcore.repository.** { *; }
-keep class com.tripian.trpcore.domain.model.** { *; }
-keep interface com.tripian.trpcore.domain.model.** { *; }
-keep enum com.tripian.trpcore.domain.model.** { *; }
-keep class com.tripian.trpcore.repository.base.** { *; }

-keep @interface retrofit2.* { *; }
-keep class retrofit2.** { *; }
-keepclassmembers interface * {
    @retrofit2.* <methods>;
}

-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep @interface okhttp3.* { *; }

-keep class com.google.gson.** { *; }
-keep @interface com.google.gson.annotations.* { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers enum * { *; }

# ============================================================================
# RXJAVA 2
# ============================================================================

-keep class io.reactivex.** { *; }
-keep interface io.reactivex.** { *; }
-keep @interface io.reactivex.* { *; }

-keepclasseswithmembernames class * {
    @io.reactivex.* <fields>;
    @io.reactivex.* <methods>;
}

# ============================================================================
# MAPBOX
# ============================================================================

-keep class com.mapbox.** { *; }
-keep interface com.mapbox.** { *; }
-keep @interface com.mapbox.* { *; }
-keep enum com.mapbox.** { *; }
-keep class mapbox.** { *; }

# ============================================================================
# EVENTBUS
# ============================================================================

-keep class org.greenrobot.eventbus.** { *; }
-keep @interface org.greenrobot.eventbus.* { *; }

-keepclasseswithmembernames class * {
    @org.greenrobot.eventbus.Subscribe <methods>;
}

-keep class * extends org.greenrobot.eventbus.event.Event { *; }

# ============================================================================
# GLIDE
# ============================================================================

-keep class com.bumptech.glide.** { *; }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

-keep class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule

# ============================================================================
# LIFECYCLE COMPONENTS
# ============================================================================

-keep class androidx.lifecycle.ViewModel { *; }
-keep class com.tripian.trpcore.base.BaseViewModel { *; }
-keepclasseswithmembernames class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

-keep class androidx.lifecycle.LiveData { *; }
-keep class androidx.lifecycle.MutableLiveData { *; }
-keep class androidx.lifecycle.Observer { *; }

# ============================================================================
# INTERNAL TRIPIAN SDKs
# ============================================================================

-keep class com.tripian.one.** { *; }
-keep interface com.tripian.one.** { *; }
-keep @interface com.tripian.one.* { *; }

-keep class com.tripian.auth.** { *; }
-keep interface com.tripian.auth.** { *; }
-keep @interface com.tripian.auth.* { *; }

-keep class com.tripian.gyg.** { *; }
-keep interface com.tripian.gyg.** { *; }
-keep @interface com.tripian.gyg.* { *; }

-keep class com.tripian.trpprovider.** { *; }
-keep interface com.tripian.trpprovider.** { *; }
-keep @interface com.tripian.trpprovider.* { *; }

-keep class com.tripian.foundation.** { *; }
-keep interface com.tripian.foundation.** { *; }

# ============================================================================
# TRPCORE UI CLASSES
# ============================================================================

-keep class com.tripian.trpcore.ui.** extends androidx.appcompat.app.AppCompatActivity { *; }
-keep class com.tripian.trpcore.ui.** extends androidx.fragment.app.Fragment { *; }
-keep class com.tripian.trpcore.ui.**ViewModel { *; }
-keep class com.tripian.trpcore.ui.**Adapter { *; }
-keep class com.tripian.trpcore.ui.**ViewHolder { *; }

# ============================================================================
# TRPCORE UTIL & BASE CLASSES
# ============================================================================

-keep class com.tripian.trpcore.util.** { *; }
-keep class com.tripian.trpcore.base.** { *; }
-keep class com.tripian.trpcore.sdk.** { *; }

-keepclassmembers enum com.tripian.trpcore.util.** { *; }
-keepclassmembers enum com.tripian.trpcore.domain.** { *; }

# ============================================================================
# ANDROID FRAMEWORK CLASSES
# ============================================================================

-keep class * extends androidx.fragment.app.FragmentFactory { *; }

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

-keepclasseswithmembernames class * implements java.io.Serializable {
  static final long serialVersionUID;
  private static final java.io.ObjectStreamField[] serialPersistentFields;
  private void writeObject(java.io.ObjectOutputStream);
  private void readObject(java.io.ObjectInputStream);
  java.lang.Object writeReplace();
  java.lang.Object readResolve();
}

# ============================================================================
# NATIVE METHODS
# ============================================================================

-keepclasseswithmembernames class * {
    native <methods>;
}

# ============================================================================
# LIBRARY-SPECIFIC RULES
# ============================================================================

-keep @interface com.google.android.material.* { *; }
-keep class com.google.firebase.crashlytics.** { *; }
-keep class com.google.android.libraries.places.** { *; }
-keep class com.google.common.** { *; }
-keep class com.google.android.flexbox.** { *; }
