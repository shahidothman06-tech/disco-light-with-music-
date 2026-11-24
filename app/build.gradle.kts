plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.yourcompany.glowparty"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.yourcompany.glowparty"   // TODO: your real package name
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        debug {
            // Google's official TEST ids. Safe to click. Never ship these.
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField("String", "ADMOB_APP_ID", "\"ca-app-pub-3940256099942544~3347511713\"")
            buildConfigField("String", "BANNER_ID", "\"ca-app-pub-3940256099942544/9214589741\"")
            buildConfigField("String", "INTERSTITIAL_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
        }
        release {
            // TODO: replace with the ids from your AdMob account before publishing.
            manifestPlaceholders["admobAppId"] = "ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY"
            buildConfigField("String", "ADMOB_APP_ID", "\"ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY\"")
            buildConfigField("String", "BANNER_ID", "\"ca-app-pub-XXXXXXXXXXXXXXXX/BANNER0000\"")
            buildConfigField("String", "INTERSTITIAL_ID", "\"ca-app-pub-XXXXXXXXXXXXXXXX/INTERST000\"")

            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.google.ads.nextgen)
    implementation(libs.google.ump)
    implementation(libs.google.play.review.ktx)
}
