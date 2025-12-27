// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Explicitly using version 8.4.0 to match your project's setup
    id("com.android.application") version "8.4.0" apply false
    id("com.android.library") version "8.4.0" apply false

    // Explicitly using Kotlin 1.9.22 to avoid conflicts with KSP
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false

    // Google Services (Optional here, but good practice to define version if used in submodules)
    id("com.google.gms.google-services") version "4.4.1" apply false
}