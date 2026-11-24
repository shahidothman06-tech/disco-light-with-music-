// Top-level build file. AGP 9 has Kotlin built in, so no kotlin-android plugin is needed.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
