# ProGuard & R8 Hardening Rules for ShellGuard-TOTP

# --- Kotlinx Serialization ---
-keepattributes *Annotation*, InnerClasses, EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer $serializer;
}
-keepclassmembers class * {
    public static final *** Companion;
}
-keepclassmembers class * {
    public static final kotlinx.serialization.KSerializer serializer(...);
}

# --- Room & SQLCipher ---
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class net.zetetic.** { *; }
-dontwarn net.zetetic.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }

# --- Ktor Client & OkHttp ---
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okio.** { *; }

# --- AndroidX Security, Biometrics & WorkManager ---
-keep class androidx.security.crypto.** { *; }
-keep class androidx.biometric.** { *; }
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# --- ShellGuard Domain & DTOs ---
-keep class com.clawstack.shellguard.totp.data.remote.models.** { *; }
-keep class com.clawstack.shellguard.totp.data.local.entities.** { *; }
-keep class com.clawstack.shellguard.totp.engine.** { *; }
-keep class com.clawstack.shellguard.totp.data.repository.** { *; }

# --- Preserve line numbers for stacktraces in release ---
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
