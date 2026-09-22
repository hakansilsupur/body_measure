plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Room writes the schema JSON here so migrations can be diffed and verified.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

/**
 * The one place the app's version is written.
 *
 * `versionCode` is derived from it rather than maintained beside it, and the
 * release workflow refuses to publish a tag that disagrees with it. Both guards
 * exist for the same reason: the in-app updater compares the running
 * `versionName` against the release tag, so if those two can drift, an update
 * can install successfully and still report itself as out of date — and the app
 * then prompts for the same version forever.
 */
val appVersionName = "1.2.0"

fun versionCodeFrom(name: String): Int {
    val parts = name.split(".")
    require(parts.size == 3) { "versionName must be major.minor.patch, got '$name'" }
    val (major, minor, patch) = parts.map {
        it.toIntOrNull() ?: throw GradleException("versionName part '$it' is not a number")
    }
    require(minor < 1000 && patch < 1000) { "minor and patch must each stay under 1000" }
    return major * 1_000_000 + minor * 1_000 + patch
}

android {
    namespace = "com.bodymeasure.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bodymeasure.app"
        minSdk = 26
        targetSdk = 35
        versionCode = versionCodeFrom(appVersionName)
        versionName = appVersionName
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        // A checked-in debug key so every build — local or CI — is signed
        // identically. Without this, each CI runner generates a throwaway
        // keystore, Android sees a different signature and refuses to update
        // in place, and the only way to install is to uninstall first, which
        // deletes the database. This is a DEBUG key only; it is not secret and
        // must never be used to sign a Play Store release.
        getByName("debug") {
            storeFile = rootProject.file("keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        // The updater reads BuildConfig.VERSION_NAME to compare itself against
        // the latest release tag. Off by default since AGP 8.
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.coil.compose)
    debugImplementation(libs.androidx.ui.tooling)
}
