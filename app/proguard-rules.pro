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
# Protect the Song model used in cache
-keep class com.demonlab.lune.tools.Song { *; }

# Protect Backup models
-keep class com.demonlab.lune.tools.PlaylistExportData { *; }
-keep class com.demonlab.lune.tools.PlaylistData { *; }
-keep class com.demonlab.lune.tools.SongMetadata { *; }

# Keep SerializedName annotations
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ==============================================================================
# JAUDIOTAGGER (Metadata extraction)
# ==============================================================================
-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**

# ==============================================================================
# COIL (Image Loading)
# ==============================================================================
-keep class coil.** { *; }
-dontwarn coil.**
-dontwarn okio.**
-dontwarn okhttp3.**

# ==============================================================================
# KOTLIN COROUTINES
# ==============================================================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}