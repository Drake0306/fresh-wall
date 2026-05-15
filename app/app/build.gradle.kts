import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Apply the google-services + crashlytics plugins only if google-services.json
// is present. The file is git-ignored (per-developer Firebase project), so CI
// and any contributor building without their own Firebase project still get
// a clean build. The in-app feedback feature degrades to the email fallback
// when Firebase isn't initialised, and Crashlytics auto-init silently no-ops
// without google-services.json.
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
    apply(plugin = libs.plugins.firebase.crashlytics.get().pluginId)
}

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val pexelsApiKey: String = localProps.getProperty("pexels.api.key", "")
val unsplashApiKey: String = localProps.getProperty("unsplash.api.key", "")
// Public URL to the wallpapers manifest hosted on Cloudflare R2 (or any
// static HTTPS endpoint). Empty until the bucket is configured — the
// RemoteWallpaperRepository treats an empty value as "unconfigured" and
// the Featured tab renders an empty state.
val wallpaperManifestUrl: String = localProps.getProperty("wallpaper.manifest.url", "")

// Release-signing creds. All gitignored — keystore file lives outside the
// repo, passwords stay in local.properties. If any of these are missing
// the release build still compiles, just unsigned (so it can't go to
// Play Store but local QA still works).
val releaseKeystorePath: String = localProps.getProperty("release.keystore.path", "")
val releaseKeystorePassword: String = localProps.getProperty("release.keystore.password", "")
val releaseKeyAlias: String = localProps.getProperty("release.key.alias", "")
val releaseKeyPassword: String = localProps.getProperty("release.key.password", "")
val hasReleaseSigning: Boolean = releaseKeystorePath.isNotBlank() &&
    file(releaseKeystorePath).exists() &&
    releaseKeystorePassword.isNotBlank() &&
    releaseKeyAlias.isNotBlank() &&
    releaseKeyPassword.isNotBlank()

android {
    namespace = "io.github.drake0306.freshwall"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "io.github.drake0306.freshwall"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.0-beta.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "PEXELS_API_KEY", "\"$pexelsApiKey\"")
        buildConfigField("String", "UNSPLASH_API_KEY", "\"$unsplashApiKey\"")
        buildConfigField("String", "WALLPAPER_MANIFEST_URL", "\"$wallpaperManifestUrl\"")
    }

    // Release signing — only registered when local.properties has all four
    // creds and the keystore file actually exists. Lets CI / fresh clones
    // build the release variant unsigned without exploding.
    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseKeystorePath)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            // isMinifyEnabled + isShrinkResources MUST flip together — the
            // resource shrinker only runs if R8 is on. We keep both false
            // until we can smoke-test a release APK on a real device.
            // proguard-rules.pro is already populated with the @Serializable
            // + WorkManager keep rules we need when R8 does get flipped on.
            isMinifyEnabled = false
            isShrinkResources = false
            // Explicit so a future build-type sweep can't accidentally
            // ship a debuggable release. The default is false but the
            // explicit declaration is the lint-friendly form.
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Pick up the release signing config when it's configured.
            // When unconfigured, leaving signingConfig null means the
            // bundle/APK is unsigned and Play Store will refuse it —
            // which is the right behaviour for a misconfigured machine.
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.play.services.ads)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.core.splashscreen)

    // Firebase — used by the in-app feedback flow. Firestore stores the
    // text body + metadata, Storage holds the optional screenshot, and
    // anonymous Auth lets the security rules require a signed-in user
    // without forcing visible sign-in. Note: as of BoM 34.x the `-ktx`
    // artifacts are gone (their Kotlin extensions merged into the main
    // libraries), so we depend on the un-suffixed coordinates.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.auth)
    // Crashlytics auto-initialises on first launch when google-services.json
    // is present. No code wiring needed — the plugin instruments the build.
    implementation(libs.firebase.crashlytics)
    implementation(libs.kotlinx.coroutines.play.services)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}