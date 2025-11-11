
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.ardeneme"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.ardeneme"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")

    // ARCore
    implementation("com.google.ar:core:1.43.0")

    // Sceneform (community fork) - ÖNEMLİ KISIM BURASI
    implementation("com.gorisse.thomas.sceneform:core:1.23.0")
    implementation("com.gorisse.thomas.sceneform:ux:1.23.0")

    // Konum için
    implementation("com.google.android.gms:play-services-location:21.3.0")
}

