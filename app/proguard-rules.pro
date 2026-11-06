# ProGuard / R8 Rules for ELI5 Android Application

# Keep all activities and application components declared in AndroidManifest
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Keep ELI5 models and data classes
-keep class com.alaotach.eli5.HistoryItem { *; }
-keep class com.alaotach.eli5.ProcessingState { *; }
-keep class com.alaotach.eli5.TextAction { *; }
-keep class com.alaotach.eli5.SettingsManager { *; }
-keep class com.alaotach.eli5.HistoryManager { *; }

# Markwon Markdown Library
-keep class io.noties.markwon.** { *; }
-dontwarn io.noties.markwon.**

# Google Play In-App Updates
-keep class com.google.android.play.core.** { *; }
-dontwarn com.google.android.play.core.**

# Android Jetpack Compose & Material 3
-keepclassmembers class * extends androidx.compose.ui.Modifier { *; }
-dontwarn androidx.compose.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Preserve Line Numbers for Crash Reporting (Play Console)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
