plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") // Use KSP for Room
}

android {
    namespace = "com.example.secretdiary"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.secretdiary"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        // Update Compose Compiler to match Kotlin 1.9.24
        kotlinCompilerExtensionVersion = "1.5.11"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        // ADD THIS PART TO YOUR EXISTING PACKAGING BLOCK
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {

    // Updated core librariesimplementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.9.0")

    // CameraX
    val cameraXVersion = "1.4.0-alpha01" // Use a specific, stable version
    implementation("androidx.camera:camera-core:${cameraXVersion}")
    implementation("androidx.camera:camera-camera2:${cameraXVersion}")
    implementation("androidx.camera:camera-lifecycle:${cameraXVersion}")
    implementation("androidx.camera:camera-view:1.3.3") // Updated to a more recent version
    // activity-ktx is already included transitively by activity-compose, but explicitly adding it is fine.
    implementation("androidx.activity:activity-ktx:1.9.0")

    // Update Compose BOM (Bill of Materials)
    // This controls all androidx.compose versions
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3") // Version managed by BOM
    implementation("androidx.compose.material:material-icons-extended") // Version managed by BOM

    // Splash Screen API (1.0.1 is still the latest stable)
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Biometrics (Fingerprint)
    // The artifact ID changed, this is the new stable KTX-inclusive library
    implementation("androidx.biometric:biometric:1.1.0")

    // Navigation (updated to latest stable)
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // ViewModel (updated to latest stable)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Room Database (updated to latest stable)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1") // Use ksp instead of kapt

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Test libraries (no major changes needed, but update BOM)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.05.00")) // Use same BOM
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
