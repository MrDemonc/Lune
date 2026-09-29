# ==============================================================================
# LUNE - PROGUARD / R8 RULES
# ==============================================================================

# Keep line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep generic attributes
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# ==============================================================================
# ROOM DATABASE
# ==============================================================================
-keep class com.demonlab.lune.data.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}

# ==============================================================================
# GSON & DATA CLASSES (Cache & Backup)
# ==============================================================================
-keep class com.demonlab.lune.tools.Song { *; }
-keep class com.demonlab.lune.tools.PlaylistExportData { *; }
-keep class com.demonlab.lune.tools.PlaylistData { *; }
-keep class com.demonlab.lune.tools.SongMetadata { *; }

-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ==============================================================================
# TOOLS (PlaybackManager, MusicService, SettingsManager, audio pipeline)
# ==============================================================================
-keep class com.demonlab.lune.tools.** { *; }

# ==============================================================================
# AUDIO EFFECTS (reflection in DynamicsEffect)
# ==============================================================================
-keep class com.demonlab.lune.audio.** { *; }

# ==============================================================================
# UI ACTIVITIES (EqualizerActivity, etc. — Compose state)
# ==============================================================================
-keep class com.demonlab.lune.ui.** { *; }

# ==============================================================================
# JAUDIOTAGGER (Metadata extraction)
# ==============================================================================
-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**

# ==============================================================================
# KOTLIN COROUTINES
# ==============================================================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ==============================================================================
# OKIO / OKHTTP (transitive deps via Coil)
# ==============================================================================
-dontwarn okio.**
-dontwarn okhttp3.**

# ==============================================================================
# AI ENGINE & MODELS (Telemetry & Serialization)
# ==============================================================================
-keep class com.demonlab.lune.ai.** { *; }
-keepclassmembers class com.demonlab.lune.ai.** { *; }

