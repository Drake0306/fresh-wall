# =============================================================================
# FreshWall — release-build ProGuard / R8 rules
# =============================================================================
# Most third-party libraries (Coil, Material 3, AdMob, Firebase) ship their
# own consumer rules and don't need entries here. The rules below cover the
# things R8 *can't* infer:
#
#   1. kotlinx.serialization reflective `Companion.serializer()` lookups on
#      our own @Serializable data classes.
#   2. WorkManager instantiating AutoRotateWorker via reflection.
#   3. Our entire `data` package (defensive — favorites JSON, manifest blobs,
#      Pexels/Unsplash wire shapes — never want field names renamed).
# =============================================================================


# ----------------------------- kotlinx.serialization -------------------------
# Source: https://github.com/Kotlin/kotlinx.serialization#android
# Keep the generated $Companion + serializer() factories so JSON decoding
# can locate the right KSerializer at runtime via reflection.

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}


# ----------------------------- our @Serializable data classes ----------------
# Belt + suspenders on top of the kotlinx rules above. The data layer holds
# the on-disk shape of favorites, category prefs, the manifest, and the wire
# shapes for Pexels/Unsplash. Renaming any field there breaks deserialisation
# of stuff users already have on their device.

-keep class io.github.drake0306.freshwall.data.** { *; }
-keepclassmembers class io.github.drake0306.freshwall.data.** { *; }


# ----------------------------- WorkManager workers ---------------------------
# WorkManager instantiates workers by fully-qualified class name; renaming
# would crash auto-rotate at the next scheduled run.

-keep class * extends androidx.work.CoroutineWorker
-keep class * extends androidx.work.Worker


# ----------------------------- crash diagnostics -----------------------------
# Keep source/line info so Crashlytics stack traces (once wired) remain
# readable. Rename the source file to "SourceFile" so we don't leak the
# original .kt path.

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
