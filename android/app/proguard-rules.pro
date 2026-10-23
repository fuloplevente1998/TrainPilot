# Jetpack Compose szabályok
-keepclassmembers class * extends androidx.compose.ui.Modifier { *; }

# Room Adatbázis entitások és DAO-k
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# Google Play Services Wearable Data Layer
-keep class com.google.android.gms.wearable.** { *; }

# Health Connect Client
-keep class androidx.health.connect.client.** { *; }
-keep class androidx.health.platform.client.** { *; }

# WorkManager Worker osztályok
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Coroutines és Kotlin Serialization / Reflection
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class * extends kotlinx.coroutines.CoroutineScope { *; }

# AndroidJUnitRunner shares this runtime dependency with the tested release.
# Its direct calls must survive R8 even when the app's own calls are inlined.
-keep class androidx.tracing.Trace { *; }
