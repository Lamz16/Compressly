# ProGuard / R8 Rules for Compressly

# Retain essential reflection and generic signature attributes
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# AndroidX DataStore Preferences
-keep class androidx.datastore.preferences.** { *; }

# PDFBox Android rules
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**
-dontwarn javax.imageio.**
-dontwarn java.awt.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Coil
-keep class coil.** { *; }
-dontwarn coil.**
