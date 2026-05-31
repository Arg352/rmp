# Proguard rules for Asylum app
-keep class com.asylum.app.models.** { *; }
-keep class com.asylum.app.api.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn okhttp3.**
-dontwarn retrofit2.**
