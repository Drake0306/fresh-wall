// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
    // Declared here so the module-level conditional `apply(plugin = ...)`
    // can resolve it. The plugin is only actually applied in app/build.gradle.kts
    // when google-services.json is present.
    alias(libs.plugins.firebase.crashlytics) apply false
}