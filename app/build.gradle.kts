import java.io.FileInputStream
import java.util.Properties

// Local, un-committed secrets (API keys) live in local.properties, same
// mechanism Android already uses for sdk.dir. See local.properties.example
// for what's expected. Never hardcode keys in source -- they'd end up in
// version control and in the APK's source maps.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(FileInputStream(f))
}
val twelveDataApiKey: String = (localProps.getProperty("TWELVE_DATA_API_KEY") ?: "").also {
    if (it.isBlank()) println("⚠ TWELVE_DATA_API_KEY not set in local.properties -- live prices will fall back to the local simulator.")
}

// Release signing reads from environment, not local.properties -- these
// are meant to come from CI secrets (see .github/workflows/release.yml:
// RELEASE_KEYSTORE_PATH is set via GITHUB_ENV after decoding the keystore
// secret; KEY_ALIAS/STORE_PASSWORD/KEY_PASSWORD are passed as step env).
// Locally, none of these are set, so the release build type is simply left
// unsigned -- still buildable for testing, just not something you'd
// distribute.
val releaseKeystorePath: String? = System.getenv("RELEASE_KEYSTORE_PATH")
val hasReleaseSigning = !releaseKeystorePath.isNullOrBlank()

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.trademaster.pro"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.trademaster.pro"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "TWELVE_DATA_API_KEY", "\"$twelveDataApiKey\"")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = System.getenv("STORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        // Material3's TopAppBar, its color defaults, chips, etc. are marked
        // @ExperimentalMaterial3Api -- using them without this opt-in is a
        // compile ERROR (not a warning) by default. Scoping it here once,
        // module-wide, beats scattering @OptIn across every composable that
        // touches the top bar, chips, or scrollable tabs.
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")

    // Local offline-first cache
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Cloud backend: Firestore mirrors into Room so every user's app sees
    // admin-published signals/posts/etc, not just their own device.
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")

    // Live market data
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
}
