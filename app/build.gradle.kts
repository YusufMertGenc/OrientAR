plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.ardeneme"
    compileSdk = 35

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
    implementation("com.google.ar:core:1.44.0") // veya 1.45.0 mevcutsa onu

    // Sceneform (1.22.0 = HDR çağrısı yok, crash yok)
    implementation("com.gorisse.thomas.sceneform:core:1.22.0")
    implementation("com.gorisse.thomas.sceneform:ux:1.22.0")

    // Konum
    implementation("com.google.android.gms:play-services-location:21.3.0")
}
