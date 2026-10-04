import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

// Secrets (Maps keys, release signing) never live in the repo. Resolution order:
// -PKEY=... > KEY env var > local.properties (gitignored). An explicit -P/env value wins
// even when blank, so `-PMAPS_API_KEY_PROD=` simulates a missing key.
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
fun secret(key: String): String =
    (providers.gradleProperty(key).orNull
        ?: providers.environmentVariable(key).orNull
        ?: localProperties.getProperty(key)
        ?: "").trim()

val mapsApiKeyDev = secret("MAPS_API_KEY_DEV")
val mapsApiKeyProd = secret("MAPS_API_KEY_PROD")

val releaseStoreFile = secret("RELEASE_STORE_FILE")
val releaseStorePassword = secret("RELEASE_STORE_PASSWORD")
val releaseKeyAlias = secret("RELEASE_KEY_ALIAS")
val releaseKeyPassword = secret("RELEASE_KEY_PASSWORD")
val hasReleaseSigning = listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword)
    .all { it.isNotEmpty() }

android {
    namespace = "com.s2aglobal.tournmate"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.s2aglobal.tournmate"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Applied to release only when all four values are configured, so local builds
    // work without a keystore (release then produces an unsigned APK/AAB).
    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            resValue("string", "app_name", "TournMate Dev")
            manifestPlaceholders["MAPS_API_KEY"] = mapsApiKeyDev
        }
        create("prod") {
            dimension = "environment"
            resValue("string", "app_name", "TournMate")
            manifestPlaceholders["MAPS_API_KEY"] = mapsApiKeyProd
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
}

// Fail any prodRelease build (assemble, bundle, install...) when the prod Maps key is
// missing, instead of shipping an app whose maps and court search silently break.
// The key is captured as a plain String, so the check is configuration-cache safe.
val checkProdMapsApiKey = tasks.register("checkProdMapsApiKey") {
    description = "Fails when MAPS_API_KEY_PROD is blank for a prod release build."
    val keyMissing = mapsApiKeyProd.isEmpty()
    doLast {
        if (keyMissing) throw GradleException(
            "MAPS_API_KEY_PROD is blank. Set it in local.properties, as an env var, " +
                "or with -PMAPS_API_KEY_PROD=... before building prodRelease."
        )
    }
}
tasks.named { it == "preProdReleaseBuild" }.configureEach {
    dependsOn(checkProdMapsApiKey)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.runtime)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation
    implementation(libs.navigation.compose)

    // Activity
    implementation(libs.activity.compose)

    // Lifecycle
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    // Core
    implementation(libs.core.ktx)
    implementation(libs.appcompat)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.lifecycle.viewmodel.compose)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    // Google Sign-In (Credential Manager)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services)
    implementation(libs.googleid)

    // DataStore
    implementation(libs.datastore.preferences)

    // Coil
    implementation(libs.coil.compose)

    // Chrome Custom Tabs
    implementation(libs.browser)

    // Health Connect
    implementation(libs.health.connect)

    // Google Maps + Places
    implementation(libs.maps.compose)
    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)
    implementation(libs.places)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(libs.coroutines.play.services)

    // Serialization
    implementation(libs.serialization.json)
}
