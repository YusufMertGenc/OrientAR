plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.ardeneme"
    compileSdk = 34 // Stabil SDK

    defaultConfig {
        applicationId = "com.example.ardeneme"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions { jvmTarget = "1.8" }
}

dependencies {
    // SceneView 2.0.3 (En Güncel ve Stabil Sürüm)
    implementation("io.github.sceneview:arsceneview:2.0.3")

    // ARCore
    implementation("com.google.ar:core:1.41.0")

    // Temel Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")

    // Konum ve Harita
    implementation("com.google.android.gms:play-services-location:21.1.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    // Lifecycle (lifecycleScope için)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
}