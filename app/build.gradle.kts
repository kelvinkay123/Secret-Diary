plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // KSP version must match Kotlin 1.9.22 specifically
    id("com.google.devtools.ksp") version "1.9.22-1.0.17"
    // Standard Google Services version
    id("com.google.gms.google-services") version "4.4.1"
}

android {
    namespace = "com.example.secretdiary"

    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.secretdiary"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildFeatures {
        compose = true
        // buildConfig = true // Uncomment if you need BuildConfig.DEBUG access
    }

    composeOptions {
        // This specific version is required for Kotlin 1.9.22
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // UPDATED: Modern Android development requires Java 17
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {
    // --- Core & Lifecycle ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)

    // --- Compose UI ---
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // --- CameraX (Video support included) ---
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.video) // Essential for your video feature
    implementation(libs.androidx.camera.view)

    // --- Firebase & Auth ---
    implementation(platform("com.google.firebase:firebase-bom:33.7.0")) // Recommended to use BOM
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // --- Local Database (Room) ---
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // --- Hardware Features ---
    implementation(libs.androidx.biometric)
    implementation(libs.google.play.services.location)

    // --- Image Loading ---
    implementation(libs.coil.compose)

    // --- Testing ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // --- Debugging ---
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}