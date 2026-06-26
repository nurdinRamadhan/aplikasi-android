# --- General Android Optimization ---
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# --- Release Logging ---
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}

# --- Kotlin & Coroutines ---
-keepattributes *Annotation*, EnclosingMethod, Signature, SourceFile, LineNumberTable
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
# Fix: Remove specific field mention that causes "The rule matches no class members"
-keepclassmembernames class kotlinx.coroutines.android.HandlerContext {
    private static *** _noNulls;
}

# --- Supabase & Kotlinx Serialization (KRUSIAL) ---
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
    @kotlinx.serialization.SerialName *;
}
-keep @kotlinx.serialization.Serializable class ** { *; }
-keep class kotlinx.serialization.json.** { *; }
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# --- Ktor Client ---
-keep class io.ktor.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
# Fix: Ignore missing management classes used by Ktor's debugger detector on Android
-dontwarn java.lang.management.**

# --- Koin Dependency Injection ---
-keep class org.koin.** { *; }

# --- Midtrans SDK ---
-keep class com.midtrans.sdk.** { *; }
-keep class com.midtrans.sdk.uikit.** { *; }
-dontwarn com.midtrans.sdk.**

# --- Coil Image Loader ---
-keep class coil.** { *; }

# --- Google Play Services (Location) ---
-keep class com.google.android.gms.location.** { *; }
-dontwarn com.google.android.gms.**

# --- Retrofit, OkHttp & Gson (VITAL FOR NETWORK) ---
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# Retrofit
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**

# Gson
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keepattributes *Annotation*

# --- Project Specific: Quran Models ---
# Melindungi model data Al-Qur'an dari obfuscation karena menggunakan Gson
-keep class com.alhasanah.alhasanahmedia.data.model.quran.** { *; }
-keepclassmembers class com.alhasanah.alhasanahmedia.data.model.quran.** { <fields>; }

# --- Project Specific: Weather Models ---
# Melindungi model cuaca BMKG dari obfuscation karena diparse Retrofit + Gson di APK release.
-keep class com.alhasanah.alhasanahmedia.data.model.weather.** { *; }
-keepclassmembers class com.alhasanah.alhasanahmedia.data.model.weather.** { <fields>; }
